package org.example.collaborative_editor.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Collaborator {
    private Long id;
    private String docId;
    private Long userId;
    private LocalDateTime createTime;
}
