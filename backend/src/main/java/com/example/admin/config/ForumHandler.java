package com.example.admin.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 论坛实时更新处理器 (广播模式)
 */
public class ForumHandler extends TextWebSocketHandler {

    // 存储所有在线的论坛会话
    private static final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 收到消息后广播给所有人 (例如：新帖子通知)
        broadcast(message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        sessions.remove(session);
    }

    /**
     * 广播消息给所有在线用户
     */
    public static void broadcast(String payload) {
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
