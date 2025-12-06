package org.example.collaborative_editor.security;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 用户认证主体
 */
@Data
@AllArgsConstructor
public class UserPrincipal {
    
    private Long userId;
    
    private String username;
}

