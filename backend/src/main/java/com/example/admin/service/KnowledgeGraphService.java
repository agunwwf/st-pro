package com.example.admin.service;

import com.example.admin.event.SkillTreeUpdateEvent;
import com.example.admin.mapper.LearningCompletionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class KnowledgeGraphService {

    @Autowired
    private LearningCompletionMapper learningCompletionMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 1. 定义有向无环图 (DAG)
    private static final Map<String, List<String>> DAG = new LinkedHashMap<>();

    static {
        DAG.put("linear", Collections.emptyList());
        DAG.put("logistic", List.of("linear"));
        DAG.put("kmeans", List.of("linear"));
        DAG.put("neural", Arrays.asList("linear", "logistic"));
        DAG.put("text", List.of("logistic"));
    }

    // 2. 获取技能树
    public List<Map<String, Object>> getSkillTree(Long userId) {
        String cacheKey = "user:skilltree:" + userId;

        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (List<Map<String, Object>>) cached;
        }

        List<Map<String, Object>> calculatedTree = calculateSkillTreeStatus(userId);
        redisTemplate.opsForValue().set(cacheKey, calculatedTree, 24, TimeUnit.HOURS);
        return calculatedTree;
    }

    // 3. 内部计算逻辑
    private List<Map<String, Object>> calculateSkillTreeStatus(Long userId) {
        List<String> completedModules = learningCompletionMapper.listByUserId(userId)
                .stream().map(c -> c.getModuleId().toLowerCase()).distinct().toList();

        List<Map<String, Object>> skillTree = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : DAG.entrySet()) {
            String module = entry.getKey();
            List<String> prerequisites = entry.getValue();

            Map<String, Object> nodeInfo = new HashMap<>();
            nodeInfo.put("moduleId", module);
            nodeInfo.put("prerequisites", prerequisites);

            if (completedModules.contains(module)) {
                nodeInfo.put("status", 2);
            } else {
                boolean isUnlocked = completedModules.containsAll(prerequisites);
                nodeInfo.put("status", isUnlocked ? 1 : 0);
            }
            skillTree.add(nodeInfo);
        }
        return skillTree;
    }

    // 4. 事件监听刷新缓存
    @Async
    @EventListener
    public void handleSkillTreeUpdate(SkillTreeUpdateEvent event) {
        Long userId = event.getUserId();
        String cacheKey = "user:skilltree:" + userId;
        redisTemplate.delete(cacheKey);
        getSkillTree(userId);
    }
}