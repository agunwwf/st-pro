package com.example.admin.controller;

import com.example.admin.entity.SysQuizScore;
import com.example.admin.mapper.SysQuizScoreMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Set;

@RestController
@RequestMapping("/api/score")
public class ScoreController {

    @Autowired
    private SysQuizScoreMapper sysQuizScoreMapper;

    // 合法的模块白名单
    private static final Set<String> VALID_MODULES = Set.of(
            "kmeans", "logistic", "neural", "linear", "text"
    );

    @GetMapping("/attempts")
    public Result<Integer> getUserAttempts(
            @RequestParam("moduleId") String moduleId,
            @RequestAttribute("userId") Long userId
    ) {
        if (moduleId == null || !VALID_MODULES.contains(moduleId)) {
            return Result.error("非法操作：系统不存在该教学模块！");
        }

        try {
            // 调用 Mapper 里写的 countUserAttempts 方法
            int attempts = sysQuizScoreMapper.countUserAttempts(userId, moduleId);
            return Result.success(attempts); // 把真实的次数返回给前端
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error("查询答题次数失败");
        }
    }

    @PostMapping("/save")
    public Result<?> saveQuizScore(
            @RequestBody QuizSubmitDTO dto,
            @RequestAttribute("userId") Long userId
    ) {
        if (dto.getModuleId() == null || !VALID_MODULES.contains(dto.getModuleId())) {
            return Result.error("非法操作：系统不存在该教学模块！");
        }

        //校验该用户在该板块是否已经考了 3 次
        int attempts = sysQuizScoreMapper.countUserAttempts(userId, dto.getModuleId());
        if (attempts >= 3) {
            return Result.error("答题失败：您已用完 3 次答题机会！");
        }

        try {
            SysQuizScore scoreRecord = new SysQuizScore();
            scoreRecord.setUserId(userId);
            scoreRecord.setModuleId(dto.getModuleId());
            scoreRecord.setScore(dto.getScore());
            scoreRecord.setAnswersDetail(dto.getAnswersDetail()); //  保存错题明细
            scoreRecord.setCreateTime(LocalDateTime.now());

            sysQuizScoreMapper.insertScore(scoreRecord);
            return Result.success("成绩同步成功");
        } catch (Exception e) {
            return Result.error("成绩保存失败...");
        }
    }
}

@Data
class QuizSubmitDTO {
    private String moduleId;
    private Integer score;
    private String answersDetail; // 传过来的 JSON 字符串
}