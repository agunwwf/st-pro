package com.example.admin.config;

import com.example.admin.entity.Message;
import com.example.admin.mapper.MessageMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.admin.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ChatHandler extends TextWebSocketHandler {

    // username -> session（简化：每个用户名保留最近一次连接）
    private static final Map<String, WebSocketSession> USER_SESSIONS = new ConcurrentHashMap<>();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final MessageMapper messageMapper;
    private final JwtUtil jwtUtil;

    public ChatHandler(MessageMapper messageMapper, JwtUtil jwtUtil) {
        this.messageMapper = messageMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("New connection: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();

        // 解析消息：auth 绑定 session；message 持久化并定向转发
        try {
            JsonNode root = OBJECT_MAPPER.readTree(payload);
            String type = root.path("type").asText();

            if ("auth".equals(type)) {
                String token = root.path("token").asText(null);
                if (token == null || token.trim().isEmpty()) {
                    session.close();
                    return;
                }
                token = token.trim();
                if (token.startsWith("Bearer ")) {
                    token = token.substring(7);
                }
                try {
                    Claims claims = jwtUtil.parseToken(token.trim());
                    String username = claims.getSubject();
                    if (username != null && !username.trim().isEmpty()) {
                        USER_SESSIONS.put(username.trim(), session);
                    } else {
                        session.close();
                    }
                } catch (Exception e) {
                    session.close();
                }
                return;
            }

            if ("message".equals(type)) {
                Message msg = new Message();
                msg.setFromUser(root.path("from").asText(null));
                msg.setToUser(root.path("to").asText(null));
                msg.setContent(root.path("content").asText(null));
                msg.setMsgType(root.path("msgType").asText("text"));

                JsonNode fileNameNode = root.path("fileName");
                if (!fileNameNode.isMissingNode() && !fileNameNode.isNull()) {
                    msg.setFileName(fileNameNode.asText());
                }

                msg.setCreateTime(LocalDateTime.now());

                // 保存到数据库，供刷新页面后读取历史记录
                if (msg.getFromUser() != null && msg.getToUser() != null && msg.getContent() != null) {
                    messageMapper.insert(msg);
                }

                // 定向发送给接收方（如果在线）
                WebSocketSession toSession = USER_SESSIONS.get(msg.getToUser());
                if (toSession != null && toSession.isOpen()) {
                    toSession.sendMessage(message);
                }
                // 也回送给发送方（多端登录时确保同步）
                WebSocketSession fromSession = USER_SESSIONS.get(msg.getFromUser());
                if (fromSession != null && fromSession.isOpen() && fromSession != toSession) {
                    fromSession.sendMessage(message);
                }
                return;
            }
        } catch (Exception e) {
            // 解析或保存失败不影响 WebSocket 正常转发
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) throws Exception {
        // 清理映射
        USER_SESSIONS.entrySet().removeIf(e -> e.getValue() == session);
        System.out.println("Connection closed: " + session.getId());
    }
}