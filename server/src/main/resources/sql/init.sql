-- ===========================
-- CoDoc 数据库初始化脚本
-- ===========================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS codoc DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE codoc;

-- ===========================
-- 用户表
-- ===========================
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ===========================
-- 文档表
-- ===========================
DROP TABLE IF EXISTS `document`;
CREATE TABLE `document` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '文档ID',
    `doc_id` VARCHAR(64) NOT NULL COMMENT '文档唯一标识',
    `title` VARCHAR(255) NOT NULL DEFAULT '无标题文档' COMMENT '文档标题',
    `content` LONGTEXT COMMENT '文档内容',
    `owner_id` BIGINT NOT NULL COMMENT '创建者ID',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-删除, 1-正常',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_id` (`doc_id`),
    KEY `idx_owner_id` (`owner_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档表';

-- ===========================
-- 文档协作者表
-- ===========================
DROP TABLE IF EXISTS `document_collaborator`;
CREATE TABLE `document_collaborator` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `doc_id` VARCHAR(64) NOT NULL COMMENT '文档标识',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `permission` TINYINT NOT NULL DEFAULT 1 COMMENT '权限: 1-只读, 2-编辑, 3-管理',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_doc_user` (`doc_id`, `user_id`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文档协作者表';

