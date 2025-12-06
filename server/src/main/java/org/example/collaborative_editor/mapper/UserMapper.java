package org.example.collaborative_editor.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.collaborative_editor.entity.User;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper {
    
    /**
     * 根据用户名查询用户
     */
    User findByUsername(@Param("username") String username);
    
    /**
     * 根据ID查询用户
     */
    User findById(@Param("id") Long id);
    
    /**
     * 插入用户
     */
    int insert(User user);
    
    /**
     * 更新用户
     */
    int update(User user);
    
    /**
     * 检查用户名是否存在
     */
    int existsByUsername(@Param("username") String username);
    
    /**
     * 检查邮箱是否存在
     */
    int existsByEmail(@Param("email") String email);
}

