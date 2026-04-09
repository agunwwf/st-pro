package com.example.admin.config;

import com.example.admin.util.JwtUtil;
import com.example.admin.controller.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.io.PrintWriter;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // OPTIONS 预检直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 因为前端的 <img src="..."> 发起的是普通 GET 请求，无法携带 Token
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/chat/file/")) {
            return true;
        }
        String token = request.getHeader("token");
        if (token == null || token.isBlank()) {
            token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7);
            }
        }
        // EventSource(SSE) 无法设置请求头，前端把 JWT 放在 query：?token=...
        if (token == null || token.isBlank()) {
            token = request.getParameter("token");
        }

        if (token == null || token.isBlank()) {
            sendUnauthorized(response, "请先登录");
            return false;
        }

        try {
            Claims claims = jwtUtil.parseToken(token);
            Long userId = claims.get("userId", Long.class);
            String username = claims.getSubject();
            request.setAttribute("userId", userId);
            request.setAttribute("username", username);
            return true;
        } catch (ExpiredJwtException e) {
            sendUnauthorized(response, "登录已过期，请重新登录");
            return false;
        } catch (JwtException e) {
            sendUnauthorized(response, "token 无效");
            return false;
        } catch (Exception e) {
            sendUnauthorized(response, "认证失败");
            return false;
        }
    }

    private void sendUnauthorized(HttpServletResponse response, String msg) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        Result<?> result = Result.error(msg);
        result.setCode(401);
        PrintWriter writer = response.getWriter();
        writer.write(OBJECT_MAPPER.writeValueAsString(result));
        writer.flush();
    }
}
