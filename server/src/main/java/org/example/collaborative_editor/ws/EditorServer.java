package org.example.collaborative_editor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.constant.WsMessageType;
import org.example.collaborative_editor.dto.WsMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 协作编辑器 WebSocket 服务端
 * 负责处理多文档的实时协作逻辑，使用 JSON 格式进行通信。
 */
@Slf4j
@Component
@ServerEndpoint("/editor/{docId}")
public class EditorServer {

    /**
     * 管理不同 docId 下的用户集合（即不同的聊天室）。
     * Key: docId
     * Value: 该文档下的所有 Session 集合
     * 使用 ConcurrentHashMap 和 CopyOnWriteArraySet 保证线程安全。
     */
    private static final Map<String, CopyOnWriteArraySet<Session>> docSessions = new ConcurrentHashMap<>();

    /**
     * Jackson ObjectMapper，用于 JSON 解析和生成。
     * 由于 WebSocket 是多例模式，需要通过静态 setter 方法注入。
     */
    private static ObjectMapper objectMapper;

    /**
     * RedisTemplate，用于操作 Redis。
     * 由于 WebSocket 是多例模式，需要通过静态 setter 方法注入。
     */
    private static RedisTemplate<String, Object> redisTemplate;

    @Autowired
    public void setObjectMapper(ObjectMapper objectMapper) {
        EditorServer.objectMapper = objectMapper;
    }

    @Autowired
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        EditorServer.redisTemplate = redisTemplate;
    }

    /**
     * 当前连接所属的文档 ID。
     * 每个 WebSocket 连接实例都有自己的 docId。
     */
    private String docId;

    /**
     * 连接建立时调用。
     *
     * @param session 当前会话
     * @param docId   路径参数，文档 ID
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("docId") String docId) {
        this.docId = docId;

        // 将用户加入对应文档的集合
        // computeIfAbsent 保证原子性：如果集合不存在则创建，存在则返回
        docSessions.computeIfAbsent(docId, k -> new CopyOnWriteArraySet<>()).add(session);

        log.info("用户 {} 加入文档 {}, 当前在线人数: {}", session.getId(), docId, docSessions.get(docId).size());

        // 从 Redis 获取文档内容
        String content = (String) redisTemplate.opsForValue().get("doc:" + docId);

        // 如果该文档已有内容，立即发送一条 type: "SYNC" 的消息给新用户
        if (content != null) {
            try {
                WsMessage syncMsg = new WsMessage();
                syncMsg.setType(WsMessageType.SYNC);
                syncMsg.setSender(WsMessageType.SENDER_SERVER);
                syncMsg.setData(content);

                String json = objectMapper.writeValueAsString(syncMsg);
                session.getAsyncRemote().sendText(json);
            } catch (IOException e) {
                log.error("发送同步消息失败", e);
            }
        }
    }

    /**
     * 收到客户端消息时调用。
     *
     * @param messageStr 收到的 JSON 消息字符串
     * @param session    发送消息的会话
     */
    @OnMessage
    public void onMessage(String messageStr, Session session) {
        try {
            // 解析收到的 JSON 消息
            WsMessage msg = objectMapper.readValue(messageStr, WsMessage.class);

            // 如果是 EDIT 类型，更新 Redis，并广播
            if (WsMessageType.EDIT.equals(msg.getType())) {
                // 更新 Redis 中的文档内容
                redisTemplate.opsForValue().set("doc:" + docId, msg.getData());

                // 标记文档为脏数据（需要同步到 MySQL）
                redisTemplate.opsForSet().add("dirty_docs", docId);

                // 广播给同文档下的其他人（排除发送者自己）
                broadcast(messageStr, session);
            }
        } catch (IOException e) {
            log.error("解析消息失败: {}", messageStr, e);
        }
    }

    /**
     * 连接关闭时调用。
     *
     * @param session 关闭的会话
     */
    @OnClose
    public void onClose(Session session) {
        CopyOnWriteArraySet<Session> sessions = docSessions.get(docId);
        if (sessions != null) {
            // 从集合中移除用户
            sessions.remove(session);
            log.info("用户 {} 离开文档 {}, 剩余在线人数: {}", session.getId(), docId, sessions.size());

            // 如果该文档下没有用户了，可以选择清理 docSessions 中的条目
            if (sessions.isEmpty()) {
                docSessions.remove(docId);
                // 注意：这里不再删除文档内容，因为内容已经持久化在 Redis 中
            }
        }
    }

    /**
     * 发生错误时调用。
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket 错误: docId={}, sessionId={}, error={}", docId, session.getId(), error.getMessage());
    }

    /**
     * 辅助方法：将 JSON 字符串发送给同组其他 Session。
     *
     * @param data   要发送的 JSON 字符串
     * @param sender 发送者的 Session（将被排除）
     */
    private void broadcast(String data, Session sender) {
        CopyOnWriteArraySet<Session> sessions = docSessions.get(docId);
        if (sessions != null) {
            for (Session s : sessions) {
                // 排除发送者自己，并且只发送给打开的连接
                if (!s.getId().equals(sender.getId()) && s.isOpen()) {
                    s.getAsyncRemote().sendText(data);
                }
            }
        }
    }
}
