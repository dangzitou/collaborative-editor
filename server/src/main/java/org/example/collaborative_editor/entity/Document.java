package org.example.collaborative_editor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 文档实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {

    private Long id;

    /**
     * 文档唯一标识
     */
    private String docId;

    private String title;

    private String content;

    /**
     * 创建者ID
     */
    private Long ownerId;

    /**
     * 状态: 0-删除, 1-正常
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}