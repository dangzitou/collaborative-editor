package org.example.collaborative_editor.constant;

/**
 * 权限常量类
 * 定义文档协作权限
 */
public class PermissionConstant {

    /**
     * 所有者权限 - 可编辑、删除、管理协作者
     */
    public static final String OWNER = "owner";

    /**
     * 编辑权限 - 可编辑文档内容
     */
    public static final String EDIT = "edit";

    /**
     * 只读权限 - 只能查看文档
     */
    public static final String VIEW = "view";

    private PermissionConstant() {
        // 私有构造方法，防止实例化
    }
}

