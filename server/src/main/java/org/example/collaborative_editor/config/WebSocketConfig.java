package org.example.collaborative_editor.config;

import lombok.RequiredArgsConstructor;
import org.example.collaborative_editor.ws.EditorServer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final EditorServer editorServer;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(editorServer, "/editor/{docId}")
                .setAllowedOrigins("*");
    }
}
