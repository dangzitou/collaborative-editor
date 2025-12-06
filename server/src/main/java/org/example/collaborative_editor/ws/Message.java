package org.example.collaborative_editor.ws;

import lombok.Data;

/**
 * WebSocket 消息模型类
 * 用于定义客户端和服务器之间通信的 JSON 数据结构。
 */
@Data
public class Message {

    /**
     * 消息类型
     * EDIT: 用户正在编辑文档内容
     * SYNC: 新用户加入时，服务器向其同步当前最新文档内容
     */
    private String type;

    /**
     * 发送者标识
     * 可以是 sessionId 或其他唯一用户ID
     */
    private String sender;

    /**
     * 消息数据
     * 对于 EDIT 类型，是文档的完整内容
     * 对于 SYNC 类型，也是文档的完整内容
     */
    private String data;
}

