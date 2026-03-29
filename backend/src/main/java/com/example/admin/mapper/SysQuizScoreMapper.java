package com.example.admin.mapper;

import com.example.admin.entity.SysQuizScore;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysQuizScoreMapper {


    // SysQuizScoreMapper.java 中新增一个查询次数的方法
    @Select("SELECT COUNT(*) FROM sys_quiz_score WHERE user_id = #{userId} AND module_id = #{moduleId}")
    int countUserAttempts(@Param("userId") Long userId, @Param("moduleId") String moduleId);

    @Insert("INSERT INTO sys_quiz_score (user_id, module_id, score, answers_detail, create_time) " +
            "VALUES (#{userId}, #{moduleId}, #{score}, #{answersDetail}, #{createTime})")
    int insertScore(SysQuizScore score);

}