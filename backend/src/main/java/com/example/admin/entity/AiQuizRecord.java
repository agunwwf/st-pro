package com.example.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class AiQuizRecord {
    private Long id;
    private Long userId;
    private String moduleId;
    private String title;
    private String weaknessAnalysis;
    private String quizJson;
    private String userAnswers;
    private Integer score;
    private Integer status;
    private LocalDateTime createTime;
}