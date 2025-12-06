package org.example.collaborative_editor.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.collaborative_editor.constant.MessageConstant;
import org.example.collaborative_editor.constant.StatusConstant;
import org.example.collaborative_editor.dto.LoginRequest;
import org.example.collaborative_editor.dto.LoginResponse;
import org.example.collaborative_editor.dto.RegisterRequest;
import org.example.collaborative_editor.entity.User;
import org.example.collaborative_editor.exception.BusinessException;
import org.example.collaborative_editor.mapper.UserMapper;
import org.example.collaborative_editor.service.UserService;
import org.example.collaborative_editor.util.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    
    @Override
    public void register(RegisterRequest request) {
        log.debug("开始注册用户: username={}", request.getUsername());

        // 检查用户名是否存在
        if (userMapper.existsByUsername(request.getUsername()) > 0) {
            log.debug("用户名已存在: {}", request.getUsername());
            throw new BusinessException(MessageConstant.USERNAME_EXIST);
        }

        // 检查邮箱是否存在
        if (request.getEmail() != null && userMapper.existsByEmail(request.getEmail()) > 0) {
            log.debug("邮箱已被注册: {}", request.getEmail());
            throw new BusinessException(MessageConstant.EMAIL_EXIST);
        }

        // 创建用户
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname() != null ? request.getNickname() : request.getUsername());
        user.setEmail(request.getEmail());
        user.setStatus(StatusConstant.ENABLE);

        userMapper.insert(user);
        log.debug("用户注册完成: username={}, userId={}", user.getUsername(), user.getId());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        log.debug("开始登录验证: username={}", request.getUsername());

        // 查询用户
        User user = userMapper.findByUsername(request.getUsername());
        if (user == null) {
            log.debug("用户不存在: {}", request.getUsername());
            throw new BusinessException(MessageConstant.PASSWORD_ERROR);
        }

        // 检查账号状态
        if (StatusConstant.DISABLE.equals(user.getStatus())) {
            log.debug("账号已禁用: {}", request.getUsername());
            throw new BusinessException(MessageConstant.ACCOUNT_DISABLED);
        }

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.debug("密码验证失败: username={}", request.getUsername());
            throw new BusinessException(MessageConstant.PASSWORD_ERROR);
        }

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.debug("JWT 生成成功: username={}, userId={}", user.getUsername(), user.getId());

        return new LoginResponse(
            token,
            user.getId(),
            user.getUsername(),
            user.getNickname(),
            user.getAvatar()
        );
    }
    
    @Override
    public User findByUsername(String username) {
        return userMapper.findByUsername(username);
    }
    
    @Override
    public User findById(Long id) {
        return userMapper.findById(id);
    }
}

