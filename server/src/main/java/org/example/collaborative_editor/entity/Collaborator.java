package org.example.collaborative_editor.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Collaborator {
    private Long id;
    private String docId;
    private Long userId;
    private LocalDateTime createTime;
}
