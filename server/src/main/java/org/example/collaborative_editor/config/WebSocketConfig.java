package org.example.collaborative_editor.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

@Configuration
@EnableWebSocket
public class WebSocketConfig {

    /**
     * 注入 ServerEndpointExporter Bean。
     *
     * 在 Spring Boot 中，如果使用内嵌的 Servlet 容器（如 Tomcat、Jetty 或 Undertow），
     * WebSocket 端点（即使用 @ServerEndpoint 注解的类）的扫描和注册默认是禁用的。
     * 这是因为标准的 Servlet 容器扫描机制不会查找 @ServerEndpoint 注解。
     *
     * ServerEndpointExporter 是 Spring 提供的一个工具类，它的作用是扫描 classpath，
     * 查找所有被 @ServerEndpoint 注解标记的类，并将它们注册为 WebSocket 端点。
     *
     * 当你将 ServerEndpointExporter 作为一个 Bean 注入到 Spring 应用上下文中时，
     * Spring Boot 会在启动时自动配置并使用它。这样，你的 WebSocket 端点就能被正确地
     * 部署到内嵌的 Servlet 容器中，并能够处理 WebSocket 连接请求。
     *
     * 简而言之，这个 Bean 是连接 Spring Boot 应用和标准 Java WebSocket API (@ServerEndpoint) 的桥梁。
     *
     * @return ServerEndpointExporter 实例
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }
}

