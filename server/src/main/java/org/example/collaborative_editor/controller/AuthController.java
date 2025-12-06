package org.example.collaborative_editor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.common.Result;
import org.example.collaborative_editor.dto.LoginRequest;
import org.example.collaborative_editor.dto.LoginResponse;
import org.example.collaborative_editor.dto.RegisterRequest;
import org.example.collaborative_editor.service.UserService;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("用户注册请求: username={}", request.getUsername());
        try {
            userService.register(request);
            log.info("用户注册成功: username={}", request.getUsername());
            return Result.success();
        } catch (Exception e) {
            log.warn("用户注册失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("用户登录请求: username={}", request.getUsername());
        try {
            LoginResponse response = userService.login(request);
            log.info("用户登录成功: username={}, userId={}", request.getUsername(), response.getUserId());
            return Result.success(response);
        } catch (Exception e) {
            log.warn("用户登录失败: username={}, reason={}", request.getUsername(), e.getMessage());
            return Result.error(e.getMessage());
        }
    }
}

