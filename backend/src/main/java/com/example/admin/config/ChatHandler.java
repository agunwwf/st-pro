package com.example.admin.config;

import com.example.admin.entity.Message;
import com.example.admin.mapper.MessageMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHandler extends TextWebSocketHandler {

    @Autowired
    private MessageMapper messageMapper;

    private static final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 连接建立
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        Map<String, Object> msgMap = objectMapper.readValue(payload, Map.class);

        String type = (String) msgMap.get("type");
        String fromUser = (String) msgMap.get("from");

        // 1. 绑定 Session
        if (fromUser != null) {
            userSessions.put(fromUser, session);
        }

        if ("message".equals(type)) {
            String toUser = (String) msgMap.get("to");
            String content = (String) msgMap.get("content");

            // 2. 保存到数据库 (持久化)
            Message dbMsg = new Message();
            dbMsg.setFromUser(fromUser);
            dbMsg.setToUser(toUser);
            dbMsg.setContent(content);
            dbMsg.setMsgType("text");
            dbMsg.setCreateTime(LocalDateTime.now());
            try {
                if (messageMapper != null) {
                    messageMapper.insert(dbMsg);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            // 3. 转发给接收者
            WebSocketSession targetSession = userSessions.get(toUser);
            if (targetSession != null && targetSession.isOpen()) {
                targetSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        userSessions.values().remove(session);
    }
}