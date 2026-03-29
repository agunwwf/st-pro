package com.example.admin.service;

import com.example.admin.entity.LearningCompletion;

import java.util.List;
import java.util.Map;

public interface LearningCompletionService {
    /** 写入或幂等更新一条完成记录（userId 来自 JWT，不信任前端传的用户 id，防越权） */
    void recordCompletion(Long userId, String moduleId, String kind);

    /**
     * Dashboard 用：totalCount = 已完成条数，maxCount = 5 项目 × 2 类型 =10，
     * items = 明细列表（可选展示）。
     */
    Map<String, Object> getSummary(Long userId);

    List<LearningCompletion> list(Long userId);
}
