package com.example.admin.config;

import com.example.admin.mapper.MessageMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Autowired
    private MessageMapper messageMapper;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 注册聊天 WebSocket
        registry.addHandler(chatHandler(), "/ws/chat")
                .setAllowedOrigins("*");

        // 注册论坛 WebSocket
        registry.addHandler(forumHandler(), "/ws/forum")
                .setAllowedOrigins("*");
    }

    @Bean
    public ChatHandler chatHandler() {
        return new ChatHandler(messageMapper);
    }

    @Bean
    public ForumHandler forumHandler() {
        return new ForumHandler();
    }
}
