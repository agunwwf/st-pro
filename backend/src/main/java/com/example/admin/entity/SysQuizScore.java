package com.example.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysQuizScore {
    private Long id;
    private Long userId;
    private String moduleId; // ST 传过来的 kmeans 等
    private Integer score;
    private String answersDetail; // ST 传过来的详尽病历 (JSON 字符串)
    private LocalDateTime createTime;
}