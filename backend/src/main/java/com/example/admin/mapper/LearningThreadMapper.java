package com.example.admin.mapper;

import com.example.admin.entity.LearningThread;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface LearningThreadMapper {
    @Insert("INSERT INTO sys_learning_thread(user_id, title, content, duration, create_time) " +
            "VALUES(#{userId}, #{title}, #{content}, #{duration}, NOW())")
    int insert(LearningThread thread);

    @Update("UPDATE sys_learning_thread SET title=#{title}, content=#{content}, duration=#{duration} WHERE id=#{id}")
    int update(LearningThread thread);

    @Select("SELECT id, user_id as userId, title, content, duration, create_time as createTime FROM sys_learning_thread WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<LearningThread> getByUserId(Long userId);

    @Select("SELECT t.id, t.user_id AS userId, COALESCE(u.nickname, u.username) AS studentName, " +
            "t.title, t.content, t.duration, t.create_time AS createTime " +
            "FROM sys_learning_thread t " +
            "JOIN sys_user u ON u.id = t.user_id " +
            "WHERE u.teacher_id = #{teacherId} AND u.role = 'STUDENT' " +
            "ORDER BY t.create_time DESC LIMIT #{limit}")
    List<Map<String, Object>> listClassActivities(@Param("teacherId") Long teacherId, @Param("limit") Integer limit);

    @Insert("INSERT INTO sys_learning_thread(user_id, title, content, duration, create_time) " +
            "VALUES(#{userId}, #{title}, #{content}, #{duration}, NOW())")
    int insertAutoActivity(@Param("userId") Long userId,
                           @Param("title") String title,
                           @Param("content") String content,
                           @Param("duration") Integer duration);

    @Select("SELECT id, user_id as userId, title, content, duration, create_time as createTime FROM sys_learning_thread WHERE user_id = #{userId} AND DATE(create_time) = CURDATE() LIMIT 1")
    LearningThread getTodayThread(Long userId);
}