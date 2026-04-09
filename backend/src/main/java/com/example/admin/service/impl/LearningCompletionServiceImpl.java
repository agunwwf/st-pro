package com.example.admin.service.impl;

import com.example.admin.entity.LearningCompletion;
import com.example.admin.event.SkillTreeUpdateEvent;
import com.example.admin.mapper.LearningCompletionMapper;
import com.example.admin.service.LearningCompletionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LearningCompletionServiceImpl implements LearningCompletionService {

    /** 允许的项目 id，与 Streamlit app.py 的 project key、Python learning_progress 常量保持一致 */
    private static final Set<String> MODULES = Set.of("kmeans", "logistic", "neural", "linear", "text");
    /** demo=演示教学完成；step=分步练习完成 */
    private static final Set<String> KINDS = Set.of("demo", "step");

    @Autowired
    private LearningCompletionMapper learningCompletionMapper;
    @Autowired(required = false)
    private org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    public void recordCompletion(Long userId, String moduleId, String kind) {
        if (userId == null || moduleId == null || kind == null) {
            throw new IllegalArgumentException("参数不完整");
        }
        String mid = moduleId.trim().toLowerCase();
        String k = kind.trim().toLowerCase();
        // 白名单校验：防止恶意拼 moduleId 刷库或 SQL 注入（MyBatis 已参数化，这里是业务安全）
        if (!MODULES.contains(mid) || !KINDS.contains(k)) {
            throw new IllegalArgumentException("moduleId 或 kind 无效");
        }
        LearningCompletion row = new LearningCompletion();
        row.setUserId(userId);
        row.setModuleId(mid);
        row.setKind(k);
        learningCompletionMapper.upsert(row);
        if (eventPublisher != null) {
            eventPublisher.publishEvent(new SkillTreeUpdateEvent(this, userId));
        }
    }

    @Override
    public Map<String, Object> getSummary(Long userId) {
        int total = learningCompletionMapper.countByUserId(userId);
        Map<String, Object> map = new HashMap<>();
        map.put("totalCount", total);
        map.put("maxCount", MODULES.size() * KINDS.size()); // 5×2=10，与分母一致
        map.put("items", learningCompletionMapper.listByUserId(userId));
        return map;
    }

    @Override
    public List<LearningCompletion> list(Long userId) {
        return learningCompletionMapper.listByUserId(userId);
    }
}
