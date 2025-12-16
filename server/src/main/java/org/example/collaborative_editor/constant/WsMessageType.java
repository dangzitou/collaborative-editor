package org.example.collaborative_editor.constant;

/**
 * WebSocket 消息类型常量
 */
public class WsMessageType {

    /**
     * 编辑消息 - 用户编辑了文档内容
     */
    public static final String EDIT = "EDIT";

    /**
     * 同步消息 - 服务器向新用户同步当前文档内容
     */
    public static final String SYNC = "SYNC";

    /**
     * 光标消息 - 同步用户光标位置（预留）
     */
    public static final String CURSOR = "CURSOR";

    /**
     * 用户加入消息（预留）
     */
    public static final String USER_JOIN = "USER_JOIN";

    /**
     * 用户离开消息（预留）
     */
    public static final String USER_LEAVE = "USER_LEAVE";

    /**
     * 用户列表消息 - 发送当前在线用户列表
     */
    public static final String USER_LIST = "USER_LIST";

    /**
     * 操作消息 - OT 算法操作（预留）
     */
    public static final String OPERATION = "OPERATION";

    /**
     * 文档删除消息
     */
    public static final String DOC_DELETED = "DOC_DELETED";

    /**
     * 系统发送者标识
     */
    public static final String SENDER_SERVER = "server";

    private WsMessageType() {
        // 私有构造方法，防止实例化
    }
}
