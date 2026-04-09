package com.example.admin.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface TeacherStudentRequestMapper {

    @Insert("INSERT INTO sys_teacher_student_request(student_id, old_teacher_id, new_teacher_id, req_type, status, approve_old, approve_new) " +
            "VALUES(#{studentId}, #{oldTeacherId}, #{newTeacherId}, #{reqType}, 'PENDING', #{approveOld}, #{approveNew})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Map<String, Object> row);

    @Select("SELECT * FROM sys_teacher_student_request WHERE id = #{id}")
    Map<String, Object> getById(@Param("id") Long id);

    @Select("SELECT COUNT(1) FROM sys_teacher_student_request " +
            "WHERE student_id = #{studentId} AND new_teacher_id = #{newTeacherId} AND req_type = #{reqType} AND status = 'PENDING'")
    int countPending(@Param("studentId") Long studentId, @Param("newTeacherId") Long newTeacherId, @Param("reqType") String reqType);

    @Select("SELECT * FROM sys_teacher_student_request " +
            "WHERE status = #{status} AND (old_teacher_id = #{teacherId} OR new_teacher_id = #{teacherId}) " +
            "ORDER BY create_time DESC")
    List<Map<String, Object>> listForTeacher(@Param("teacherId") Long teacherId, @Param("status") String status);

    @Update("UPDATE sys_teacher_student_request " +
            "SET approve_old = CASE WHEN old_teacher_id = #{teacherId} THEN 1 ELSE approve_old END, " +
            "    approve_new = CASE WHEN new_teacher_id = #{teacherId} THEN 1 ELSE approve_new END " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int approveByTeacher(@Param("id") Long id, @Param("teacherId") Long teacherId);

    @Update("UPDATE sys_teacher_student_request SET status = 'REJECTED' WHERE id = #{id} AND status = 'PENDING'")
    int reject(@Param("id") Long id);

    @Update("UPDATE sys_teacher_student_request SET status = 'APPROVED' WHERE id = #{id} AND status = 'PENDING'")
    int markApproved(@Param("id") Long id);
}

