package com.example.admin.mapper;

import com.example.admin.entity.SysQuizScore;
import org.apache.ibatis.annotations.*;

@Mapper
public interface ScoreMapper {

    // 检查这个用户是否已经做过这个模块了 (防作弊)
    @Select("SELECT COUNT(*) FROM sys_quiz_score WHERE user_id = #{userId} AND module_id = #{moduleId}")
    int checkIsCompleted(@Param("userId") Long userId, @Param("moduleId") String moduleId);

    // 存入新成绩
    @Insert("INSERT INTO sys_quiz_score (user_id, module_id, score, answers_detail, create_time) " +
            "VALUES (#{userId}, #{moduleId}, #{score}, #{answersDetail}, NOW())")
    int insertScore(SysQuizScore score);

    // 读取该用户在该模块的首次/唯一成绩详情（用于 ST 锁定后展示首提结果）
    @Select("SELECT id, user_id AS userId, module_id AS moduleId, score, " +
            "CAST(answers_detail AS CHAR CHARACTER SET utf8mb4) AS answersDetail, create_time AS createTime " +
            "FROM sys_quiz_score WHERE user_id = #{userId} AND module_id = #{moduleId} LIMIT 1")
    SysQuizScore getScoreDetail(@Param("userId") Long userId, @Param("moduleId") String moduleId);
}