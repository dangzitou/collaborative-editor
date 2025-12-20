package org.example.collaborative_editor.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentVO {
    private Long id;
    private String docId;
    private String title;
    private String content;
    private Long ownerId;
    private String ownerName; // 新增字段
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
