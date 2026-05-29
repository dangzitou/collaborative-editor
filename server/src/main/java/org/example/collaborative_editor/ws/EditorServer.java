package org.example.collaborative_editor.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.example.collaborative_editor.constant.MessageConstant;
import org.example.collaborative_editor.constant.WsMessageType;
import org.example.collaborative_editor.dto.WsMessage;
import org.example.collaborative_editor.entity.Collaborator;
import org.example.collaborative_editor.entity.Document;
import org.example.collaborative_editor.mapper.CollaboratorMapper;
import org.example.collaborative_editor.service.DocumentService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 协作编辑器 WebSocket 服务端（Spring TextWebSocketHandler）
 * 负责处理多文档的实时协作逻辑，使用 JSON 格式进行通信。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EditorServer extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;
    private final CollaboratorMapper collaboratorMapper;
    private final DocumentService documentService;
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * 管理不同 docId 下的用户集合。
     * Key: docId, Value: 该文档下的所有 WebSocketSession 集合
     */
    private static final Map<String, CopyOnWriteArraySet<WebSocketSession>> docSessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        // 从 URI 路径提取 docId: /editor/{docId}
        String path = uri.getPath();
        String docId = extractDocId(path);
        if (docId == null || docId.isEmpty()) {
            session.close(new CloseStatus(4001, MessageConstant.DOCUMENT_NOT_FOUND));
            return;
        }

        // 解析查询参数
        Map<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
        String ticket = params.get("ticket");
        String queryUsername = params.get("username");

        // 验证票据
        if (ticket == null) {
            session.close(new CloseStatus(4003, "Missing Ticket"));
            return;
        }

        Long userId;
        String username;
        try {
            String redisKey = "ws:ticket:" + ticket;
            @SuppressWarnings("unchecked")
            Map<String, Object> ticketData = (Map<String, Object>) redisTemplate.opsForValue().get(redisKey);
            if (ticketData == null) {
                log.warn("WebSocket 票据无效或已过期: {}", ticket);
                session.close(new CloseStatus(4003, "Invalid Ticket"));
                return;
            }
            redisTemplate.delete(redisKey);

            userId = Long.valueOf(ticketData.get("userId").toString());
            username = (String) ticketData.get("username");

            // 权限检查
            Document document = documentService.getDocument(docId);
            if (document == null) {
                session.close(new CloseStatus(4004, MessageConstant.DOCUMENT_NOT_FOUND));
                return;
            }
            if (!document.getOwnerId().equals(userId)) {
                Collaborator collaborator = collaboratorMapper.getByDocIdAndUserId(docId, userId);
                if (collaborator == null) {
                    session.close(new CloseStatus(4003, MessageConstant.DOCUMENT_NO_PERMISSION));
                    return;
                }
            }

            if (queryUsername != null && !queryUsername.isEmpty()) {
                username = queryUsername;
            }
        } catch (Exception e) {
            log.warn("WebSocket 票据验证异常: {}", e.getMessage());
            session.close(new CloseStatus(4003, "Invalid Ticket"));
            return;
        }

        // 存储会话属性
        session.getAttributes().put("docId", docId);
        session.getAttributes().put("userId", userId);
        session.getAttributes().put("username", username);

        // 从 Redis 获取文档内容，若无则从数据库加载
        String content = (String) redisTemplate.opsForValue().get("doc:" + docId);
        if (content == null) {
            try {
                Document doc = documentService.getDocument(docId);
                if (doc != null) {
                    content = doc.getContent();
                    if (content == null) content = "";
                    redisTemplate.opsForValue().set("doc:" + docId, content, 24, TimeUnit.HOURS);
                }
            } catch (Exception e) {
                log.warn("加载文档失败: {}", docId);
                session.close(new CloseStatus(4004, MessageConstant.DOCUMENT_NOT_FOUND));
                return;
            }
        }

        // 加入文档房间
        docSessions.computeIfAbsent(docId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("用户 {} (ID:{}) 加入文档 {}, 当前在线人数: {}", username, userId, docId, docSessions.get(docId).size());

        // 广播用户加入
        try {
            WsMessage joinMsg = new WsMessage();
            joinMsg.setType(WsMessageType.USER_JOIN);
            joinMsg.setSender(username);
            broadcast(objectMapper.writeValueAsString(joinMsg), session);
        } catch (Exception e) {
            log.error("广播用户加入消息失败", e);
        }

        // 发送在线用户列表
        try {
            List<String> userList = docSessions.get(docId).stream()
                    .map(s -> (String) s.getAttributes().get("username"))
                    .filter(n -> n != null)
                    .distinct()
                    .collect(Collectors.toList());

            WsMessage listMsg = new WsMessage();
            listMsg.setType(WsMessageType.USER_LIST);
            listMsg.setSender(WsMessageType.SENDER_SERVER);
            listMsg.setData(objectMapper.writeValueAsString(userList));
            synchronized (session) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(listMsg)));
            }
        } catch (Exception e) {
            log.error("发送用户列表失败", e);
        }

        // 发送文档内容同步
        if (content != null) {
            try {
                WsMessage syncMsg = new WsMessage();
                syncMsg.setType(WsMessageType.SYNC);
                syncMsg.setSender(WsMessageType.SENDER_SERVER);
                syncMsg.setData(content);
                synchronized (session) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(syncMsg)));
                }
            } catch (Exception e) {
                log.error("发送同步消息失败", e);
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String docId = (String) session.getAttributes().get("docId");
        if (docId == null) return;

        String payload = message.getPayload();
        try {
            WsMessage msg = objectMapper.readValue(payload, WsMessage.class);

            if (WsMessageType.EDIT.equals(msg.getType())) {
                redisTemplate.opsForValue().set("doc:" + docId, msg.getData());
                redisTemplate.opsForSet().add("dirty_docs", docId);
                broadcast(payload, session);
            } else if (WsMessageType.CURSOR.equals(msg.getType())) {
                String username = (String) session.getAttributes().get("username");
                msg.setSender(username);
                broadcast(objectMapper.writeValueAsString(msg), session);
            } else if ("PING".equals(msg.getType())) {
                WsMessage pong = new WsMessage();
                pong.setType("PONG");
                pong.setSender(WsMessageType.SENDER_SERVER);
                pong.setData("pong");
                synchronized (session) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
                }
            }
        } catch (Exception e) {
            log.error("解析消息失败: {}", payload, e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String docId = (String) session.getAttributes().get("docId");
        String username = (String) session.getAttributes().get("username");
        if (docId == null) return;

        CopyOnWriteArraySet<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions != null) {
            sessions.remove(session);
            log.info("用户 {} 离开文档 {}, 剩余在线人数: {}", username != null ? username : session.getId(), docId, sessions.size());

            if (username != null) {
                try {
                    WsMessage leaveMsg = new WsMessage();
                    leaveMsg.setType(WsMessageType.USER_LEAVE);
                    leaveMsg.setSender(username);
                    broadcast(objectMapper.writeValueAsString(leaveMsg), session);
                } catch (Exception e) {
                    log.error("广播用户离开消息失败", e);
                }
            }

            if (sessions.isEmpty()) {
                docSessions.remove(docId);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String docId = (String) session.getAttributes().get("docId");
        log.error("WebSocket 错误: docId={}, sessionId={}, error={}", docId, session.getId(), exception.getMessage());
    }

    private void broadcast(String data, WebSocketSession sender) {
        String docId = (String) sender.getAttributes().get("docId");
        if (docId == null) return;

        CopyOnWriteArraySet<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions != null) {
            for (WebSocketSession s : sessions) {
                if (!s.getId().equals(sender.getId()) && s.isOpen()) {
                    try {
                        synchronized (s) {
                            s.sendMessage(new TextMessage(data));
                        }
                    } catch (Exception e) {
                        log.error("广播消息失败: sessionId={}", s.getId(), e);
                    }
                }
            }
        }
    }

    /**
     * 向指定文档的所有用户广播系统消息
     */
    public void broadcastSystemMessage(String docId, String type, String content) {
        CopyOnWriteArraySet<WebSocketSession> sessions = docSessions.get(docId);
        if (sessions != null) {
            try {
                WsMessage msg = new WsMessage();
                msg.setType(type);
                msg.setSender(WsMessageType.SENDER_SERVER);
                msg.setData(content);
                String json = objectMapper.writeValueAsString(msg);

                for (WebSocketSession s : sessions) {
                    if (s.isOpen()) {
                        synchronized (s) {
                            s.sendMessage(new TextMessage(json));
                        }
                    }
                }
            } catch (Exception e) {
                log.error("广播系统消息失败: docId={}", docId, e);
            }
        }
    }

    private String extractDocId(String path) {
        // path 格式: /editor/{docId}
        String prefix = "/editor/";
        int idx = path.indexOf(prefix);
        if (idx >= 0) {
            return path.substring(idx + prefix.length());
        }
        return null;
    }
}
