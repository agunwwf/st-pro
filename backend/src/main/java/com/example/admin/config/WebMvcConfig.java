package com.example.admin.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/user/login", "/api/user/register");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath().normalize();
        try {
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
        } catch (IOException ignored) {
            // 目录创建失败时仍注册路径，上传接口会再次尝试创建
        }
        String loc = uploadDir.toUri().toString();
        if (!loc.endsWith("/")) {
            loc += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(loc);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许前端开发环境访问所有 /api/** 接口和 /uploads/**
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);

        registry.addMapping("/uploads/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*");
    }
}

