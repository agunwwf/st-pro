package com.example.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiChat {
    private Long id;
    private Long userId;
    private String role;
    private String content;
    private LocalDateTime createTime;
}