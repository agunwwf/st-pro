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

    @Select("SELECT r.id, r.student_id, r.old_teacher_id, r.new_teacher_id, r.req_type, r.status, r.approve_old, r.approve_new, r.create_time, " +
            "       s.username AS student_username, s.nickname AS student_nickname, s.avatar AS student_avatar, " +
            "       oldt.username AS old_teacher_username, oldt.nickname AS old_teacher_nickname, " +
            "       newt.username AS new_teacher_username, newt.nickname AS new_teacher_nickname " +
            "FROM sys_teacher_student_request r " +
            "LEFT JOIN sys_user s ON s.id = r.student_id " +
            "LEFT JOIN sys_user oldt ON oldt.id = r.old_teacher_id " +
            "LEFT JOIN sys_user newt ON newt.id = r.new_teacher_id " +
            "WHERE (#{status} IS NULL OR #{status} = '' OR r.status = #{status}) " +
            "  AND (r.old_teacher_id = #{teacherId} OR r.new_teacher_id = #{teacherId}) " +
            "ORDER BY r.create_time DESC")
    List<Map<String, Object>> listForTeacher(@Param("teacherId") Long teacherId, @Param("status") String status);

    @Update("UPDATE sys_teacher_student_request " +
            "SET approve_old = CASE WHEN old_teacher_id = #{teacherId} THEN 1 ELSE approve_old END, " +
            "    approve_new = CASE WHEN new_teacher_id = #{teacherId} THEN 1 ELSE approve_new END " +
            "WHERE id = #{id} AND status = 'PENDING'")
    int approveByTeacher(@Param("id") Long id, @Param("teacherId") Long teacherId);

    @Update("UPDATE sys_teacher_student_request SET status = 'REJECTED' WHERE id = #{id} AND status = 'PENDING'")
    int reject(@Param("id") Long id);

    @Delete("DELETE FROM sys_teacher_student_request " +
            "WHERE id <> #{id} AND student_id = #{studentId} AND new_teacher_id = #{newTeacherId} " +
            "AND req_type = #{reqType} AND status = 'REJECTED'")
    int deleteDuplicateRejected(@Param("id") Long id,
                                @Param("studentId") Long studentId,
                                @Param("newTeacherId") Long newTeacherId,
                                @Param("reqType") String reqType);

    @Update("UPDATE sys_teacher_student_request SET status = 'APPROVED' WHERE id = #{id} AND status = 'PENDING'")
    int markApproved(@Param("id") Long id);
}

