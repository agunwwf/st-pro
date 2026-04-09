package com.example.admin.entity;

import lombok.Data;
import java.util.Date;
import java.util.List;

public class LmsModels {

    @Data
    public static class Question {
        private Long id;
        private Long teacherId;
        private String category;
        private String type;
        private String content;
        private String options; // 数据库里的 JSON 字符串
        private String standardAnswer;
    }

    @Data
    public static class Paper {
        private Long id;
        private Long teacherId;
        private String title;
        private String category;
        private Integer totalScore;
        private Date createTime;
        // 附带信息，用于前端展示
        private Integer questionCount;
    }

    @Data
    public static class Assignment {
        private Long id;
        private Long teacherId;
        private Long paperId;
        private String publishName;
        private Date startTime;
        private Date endTime;
        private Integer timeLimitMinutes;
        // 附带信息
        private String paperTitle;
    }

    // --- 接收前端传来的表单数据 ---
    @Data
    public static class PaperCreateReq {
        private String title;
        private String category;
        private List<Long> questionIds;
    }

    @Data
    public static class AssignmentPublishReq {
        private Long paperId;
        private String publishName;
        private Date startTime;
        private Date endTime;
        private Integer timeLimitMinutes;
    }
}