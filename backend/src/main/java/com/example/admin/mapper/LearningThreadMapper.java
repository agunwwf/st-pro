package com.example.admin.mapper;

import com.example.admin.entity.LearningThread;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface LearningThreadMapper {
    @Insert("INSERT INTO sys_learning_thread(user_id, title, content, duration, create_time) " +
            "VALUES(#{userId}, #{title}, #{content}, #{duration}, NOW())")
    int insert(LearningThread thread);

    @Update("UPDATE sys_learning_thread SET title=#{title}, content=#{content}, duration=#{duration} WHERE id=#{id}")
    int update(LearningThread thread);

    @Select("SELECT id, user_id as userId, title, content, duration, create_time as createTime FROM sys_learning_thread WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<LearningThread> getByUserId(Long userId);

    @Select("SELECT id, user_id as userId, title, content, duration, create_time as createTime FROM sys_learning_thread WHERE user_id = #{userId} AND DATE(create_time) = CURDATE() LIMIT 1")
    LearningThread getTodayThread(Long userId);
}