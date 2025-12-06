package org.example.collaborative_editor.service;

import org.example.collaborative_editor.dto.LoginRequest;
import org.example.collaborative_editor.dto.LoginResponse;
import org.example.collaborative_editor.dto.RegisterRequest;
import org.example.collaborative_editor.entity.User;

/**
 * 用户服务接口
 */
public interface UserService {
    
    /**
     * 用户注册
     */
    void register(RegisterRequest request);
    
    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(String username);
    
    /**
     * 根据ID查询用户
     */
    User findById(Long id);
}

