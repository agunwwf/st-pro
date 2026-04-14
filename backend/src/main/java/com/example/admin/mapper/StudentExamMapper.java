package com.example.admin.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface StudentExamMapper {

    @Select("SELECT teacher_id FROM sys_user WHERE id = #{studentId}")
    Long getStudentTeacherId(@Param("studentId") Long studentId);

    @Select("SELECT a.id, a.publish_name as publishName, a.start_time as startTime, a.end_time as endTime, " +
            "a.time_limit_minutes as timeLimitMinutes, p.title as paperTitle, " +
            "r.status, r.score, r.submit_time as submitTime " +
            "FROM sys_assignment a " +
            "JOIN sys_user u ON u.id = #{studentId} " +
            "JOIN sys_paper p ON a.paper_id = p.id " +
            "LEFT JOIN sys_student_record r ON a.id = r.assignment_id AND r.student_id = u.id " +
            "WHERE a.teacher_id = u.teacher_id ORDER BY a.create_time DESC")
    List<Map<String, Object>> listMyExams(Long studentId);

    @Select({
            "<script>",
            "SELECT a.id, a.publish_name as publishName, a.start_time as startTime, a.end_time as endTime,",
            "a.time_limit_minutes as timeLimitMinutes, p.title as paperTitle,",
            "r.status, r.score, r.submit_time as submitTime",
            "FROM sys_assignment a",
            "JOIN sys_user u ON u.id = #{studentId}",
            "JOIN sys_paper p ON a.paper_id = p.id",
            "LEFT JOIN sys_student_record r ON a.id = r.assignment_id AND r.student_id = u.id",
            "WHERE a.teacher_id = u.teacher_id",
            "<if test='teacherId != null'>",
            "AND a.teacher_id = #{teacherId}",
            "</if>",
            "ORDER BY a.create_time DESC",
            "</script>"
    })
    List<Map<String, Object>> listMyExamsByTeacher(@Param("studentId") Long studentId, @Param("teacherId") Long teacherId);

    @Select("SELECT a.id as assignmentId, a.publish_name as publishName, a.time_limit_minutes as timeLimitMinutes, p.title as paperTitle " +
            "FROM sys_assignment a JOIN sys_paper p ON a.paper_id = p.id WHERE a.id = #{assignmentId}")
    Map<String, Object> getExamDetail(Long assignmentId);

    @Select("SELECT a.publish_name as publishName, p.title as paperTitle " +
            "FROM sys_assignment a JOIN sys_paper p ON a.paper_id = p.id WHERE a.id = #{assignmentId}")
    Map<String, Object> getAssignmentBrief(@Param("assignmentId") Long assignmentId);

    // 学生只能查看自己导师下发的考试任务
    @Select("SELECT COUNT(1) FROM sys_assignment a " +
            "JOIN sys_user u ON u.id = #{studentId} " +
            "WHERE a.id = #{assignmentId} AND u.teacher_id = a.teacher_id")
    int canAccessAssignment(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);


    @Select("SELECT start_time AS startTime, end_time AS endTime, time_limit_minutes AS timeLimitMinutes " +
            "FROM sys_assignment WHERE id = #{assignmentId}")
    Map<String, Object> getAssignmentWindow(@Param("assignmentId") Long assignmentId);

    // 防重复提交：已交卷(>=1)则禁止再次覆盖答案
    @Select("SELECT status FROM sys_student_record WHERE assignment_id = #{assignmentId} AND student_id = #{studentId}")
    Integer getStudentRecordStatus(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

    @Select("SELECT CAST(student_answers AS CHAR CHARACTER SET utf8mb4) AS answersJson, score, status, submit_time AS submitTime " +
            "FROM sys_student_record WHERE assignment_id = #{assignmentId} AND student_id = #{studentId}")
    Map<String, Object> getStudentRecord(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId);

    @Select("SELECT q.id, q.type, q.category, q.content, q.options, pq.score " +
            "FROM sys_question q JOIN sys_paper_question pq ON q.id = pq.question_id " +
            "JOIN sys_assignment a ON pq.paper_id = a.paper_id WHERE a.id = #{assignmentId}")
    List<Map<String, Object>> listExamQuestions(Long assignmentId);

    @Select("SELECT q.id, q.type, q.category, q.content, q.options, q.standard_answer AS standardAnswer, pq.score " +
            "FROM sys_question q JOIN sys_paper_question pq ON q.id = pq.question_id " +
            "JOIN sys_assignment a ON pq.paper_id = a.paper_id WHERE a.id = #{assignmentId}")
    List<Map<String, Object>> listExamQuestionsWithAnswer(Long assignmentId);

    @Insert("INSERT INTO sys_student_record(assignment_id, student_id, status, score, student_answers, submit_time) " +
            "VALUES(#{assignmentId}, #{studentId}, 1, #{score}, #{answers}, NOW()) " +
            "ON DUPLICATE KEY UPDATE status = 1, score = #{score}, student_answers = #{answers}, submit_time = NOW()")
    void submitExamRecord(@Param("assignmentId") Long assignmentId, @Param("studentId") Long studentId, @Param("answers") String answers, @Param("score") Integer score);
}