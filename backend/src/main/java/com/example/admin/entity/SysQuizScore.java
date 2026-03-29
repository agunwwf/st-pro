package com.example.admin.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SysQuizScore {
    private Long id;
    private Long userId;
    private String moduleId;
    private Integer score;
    //用于接收并存储前端发来的错题/答题明细 JSON 字符串
    private String answersDetail;
    private LocalDateTime createTime;
}