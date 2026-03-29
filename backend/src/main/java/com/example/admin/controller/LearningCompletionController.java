package com.example.admin.controller;

import com.example.admin.service.LearningCompletionService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

/**
 * 教学完成「打卡」API。
 * 走全局 JWT 拦截器：必须在请求头带 token（Streamlit 用 requests 发 JSON；Vue 用 axios 拦截器自动带 token）。
 */
@RestController
@RequestMapping("/api/learning")
@CrossOrigin
public class LearningCompletionController {

    @Autowired
    private LearningCompletionService learningCompletionService;

    /**
     * 记录一次完成。Body 示例：{"moduleId":"kmeans","kind":"demo"}
     * userId 从 req.getAttribute("userId") 取，是登录时 JWT 解析结果，不能由前端随便指定。
     */
    @PostMapping("/complete")
    public Result<Map<String, Boolean>> complete(HttpServletRequest req, @RequestBody Map<String, String> body) {
        Long userId = (Long) req.getAttribute("userId");
        try {
            learningCompletionService.recordCompletion(
                    userId,
                    body.get("moduleId"),
                    body.get("kind"));
            // 用 Map 而不是 Result.success("字符串")，避免 Java 泛型 success 方法重载二义性
            return Result.success(Collections.singletonMap("ok", true));
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /** Dashboard 拉取：已完成数量 + 明细 + 满分母 10 */
    @GetMapping("/summary")
    public Result<Map<String, Object>> summary(HttpServletRequest req) {
        Long userId = (Long) req.getAttribute("userId");
        return Result.success(learningCompletionService.getSummary(userId));
    }
}
