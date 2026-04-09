package com.example.admin.mapper;

import org.apache.ibatis.annotations.*;

import java.util.Map;

@Mapper
public interface TeacherStudentBlockMapper {

    @Select("SELECT active FROM sys_teacher_student_block WHERE teacher_id = #{teacherId} AND student_id = #{studentId}")
    Integer getActive(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);

    @Insert("INSERT INTO sys_teacher_student_block(teacher_id, student_id, reason, active, created_by) " +
            "VALUES(#{teacherId}, #{studentId}, #{reason}, 1, #{createdBy}) " +
            "ON DUPLICATE KEY UPDATE active = 1, reason = #{reason}, created_by = #{createdBy}, blocked_at = NOW()")
    int upsertActive(Map<String, Object> row);

    @Update("UPDATE sys_teacher_student_block SET active = 0 WHERE teacher_id = #{teacherId} AND student_id = #{studentId}")
    int deactivate(@Param("teacherId") Long teacherId, @Param("studentId") Long studentId);
}

