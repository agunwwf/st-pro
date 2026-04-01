package com.example.admin.controller;

import com.example.admin.entity.SysQuizScore;
import com.example.admin.mapper.ScoreMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private ScoreMapper scoreMapper;

    // 1. ST 页面一加载，就会调这个接口看通道要不要锁定
    @GetMapping("/check")
    public Result checkCompleted(@RequestAttribute("userId") Long userId, @RequestParam String moduleId) {
        int count = scoreMapper.checkIsCompleted(userId, moduleId);
        // 如果 count > 0，说明做过了，返回 true 告诉 ST 锁定界面
        return Result.success(count > 0); 
    }

    // 2. ST 点击提交试卷时，调这个接口存档
    @PostMapping("/save")
    public Result saveScore(@RequestBody SysQuizScore score, @RequestAttribute("userId") Long userId) {
        score.setUserId(userId); // 强制绑定当前登录用户的 ID，防止越权
        try {
            scoreMapper.insertScore(score);
            return Result.success("成绩已成功存档");
        } catch (DuplicateKeyException e) {
            // 如果学生试图抓包绕过前端重复提交，触发了数据库的 UNIQUE KEY，就会被这里无情拦截
            return Result.error("您已提交过该模块的测验，请勿重复提交");
        } catch (Exception e) {
            return Result.error("存档失败，服务器内部错误");
        }
    }

    // 3. ST 锁定后读取首提成绩与答题详情（用于展示“成绩 + 标准答案 + 你的答案 + 解析”）
    @GetMapping("/detail")
    public Result getScoreDetail(@RequestAttribute("userId") Long userId, @RequestParam String moduleId) {
        SysQuizScore detail = scoreMapper.getScoreDetail(userId, moduleId);
        if (detail == null) {
            return Result.error("未找到该模块成绩");
        }
        return Result.success(detail);
    }
}