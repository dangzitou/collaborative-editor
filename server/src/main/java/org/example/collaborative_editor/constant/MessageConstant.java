package org.example.collaborative_editor.constant;

/**
 * 消息常量类
 * 定义系统中使用的提示信息
 */
public class MessageConstant {

    // ========== 用户相关 ==========
    public static final String USERNAME_EXIST = "用户名已存在";
    public static final String EMAIL_EXIST = "邮箱已被注册";
    public static final String USER_NOT_FOUND = "用户不存在";
    public static final String PASSWORD_ERROR = "用户名或密码错误";
    public static final String ACCOUNT_DISABLED = "账号已被禁用";
    public static final String LOGIN_SUCCESS = "登录成功";
    public static final String REGISTER_SUCCESS = "注册成功";

    // ========== 认证相关 ==========
    public static final String TOKEN_INVALID = "无效的令牌";
    public static final String TOKEN_EXPIRED = "令牌已过期";
    public static final String UNAUTHORIZED = "未授权，请先登录";
    public static final String ACCESS_DENIED = "权限不足";

    // ========== 文档相关 ==========
    public static final String DOCUMENT_NOT_FOUND = "文档不存在";
    public static final String DOCUMENT_CREATE_SUCCESS = "文档创建成功";
    public static final String DOCUMENT_UPDATE_SUCCESS = "文档更新成功";
    public static final String DOCUMENT_DELETE_SUCCESS = "文档删除成功";
    public static final String DOCUMENT_NO_PERMISSION = "无权限操作此文档";

    // ========== 协作相关 ==========
    public static final String INVITE_CODE_INVALID = "邀请码无效或已过期";
    public static final String COLLABORATOR_EXIST = "该用户已是协作者";
    public static final String COLLABORATOR_NOT_FOUND = "协作者不存在";
    public static final String COLLABORATOR_ADD_SUCCESS = "添加协作者成功";
    public static final String COLLABORATOR_REMOVE_SUCCESS = "移除协作者成功";
    public static final String CANNOT_REMOVE_OWNER = "不能移除文档所有者";

    // ========== 通用 ==========
    public static final String SUCCESS = "操作成功";
    public static final String FAILED = "操作失败";
    public static final String PARAM_ERROR = "参数错误";
    public static final String SYSTEM_ERROR = "系统异常，请稍后重试";

    private MessageConstant() {
        // 私有构造方法，防止实例化
    }
}
