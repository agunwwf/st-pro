package com.example.admin.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Map;

@Mapper
public interface TeacherAdminMapper {

    @Select("SELECT id, username, nickname FROM sys_user WHERE role = 'ADMIN'")
    List<Map<String, Object>> listAllTeachers();

    @Select("SELECT id, username, nickname, avatar, role, " +
            "IFNULL(ROUND((SELECT COUNT(*) FROM sys_learning_completion WHERE user_id = sys_user.id) / 10.0 * 100), 0) AS progress, " +
            "check_in_count as checkInCount, is_model as isModel, create_time as createTime " +
            "FROM sys_user WHERE teacher_id = #{teacherId} AND role = 'STUDENT'")
    List<Map<String, Object>> listMyStudents(Long teacherId);

    // 题库列表：分页 + 筛选，避免一次性全量导致慢/易被爬
    @Select({
            "<script>",
            "SELECT id, category, type, content,",
            "CAST(options AS CHAR CHARACTER SET utf8mb4) AS options,",
            "standard_answer AS standardAnswer, create_time AS createTime",
            "FROM sys_question",
            "WHERE 1=1",
            "<if test='category != null and category != \"\"'>",
            "AND category = #{category}",
            "</if>",
            "<if test='type != null and type != \"\" and type != \"ALL\"'>",
            "AND type = #{type}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND content LIKE CONCAT('%', #{keyword}, '%')",
            "</if>",
            "ORDER BY id DESC",
            "LIMIT #{limit} OFFSET #{offset}",
            "</script>"
    })
    List<Map<String, Object>> listQuestionsPaged(
            @Param("offset") int offset,
            @Param("limit") int limit,
            @Param("category") String category,
            @Param("type") String type,
            @Param("keyword") String keyword
    );

    @Select({
            "<script>",
            "SELECT COUNT(1) FROM sys_question",
            "WHERE 1=1",
            "<if test='category != null and category != \"\"'>",
            "AND category = #{category}",
            "</if>",
            "<if test='type != null and type != \"\" and type != \"ALL\"'>",
            "AND type = #{type}",
            "</if>",
            "<if test='keyword != null and keyword != \"\"'>",
            "AND content LIKE CONCAT('%', #{keyword}, '%')",
            "</if>",
            "</script>"
    })
    long countQuestions(@Param("category") String category, @Param("type") String type, @Param("keyword") String keyword);

    @Insert("INSERT INTO sys_paper(teacher_id, title, category, total_score) VALUES(#{teacherId}, #{title}, #{category}, 100)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertPaper(Map<String, Object> paper);

    @Insert("INSERT INTO sys_paper_question(paper_id, question_id, score) VALUES(#{paperId}, #{questionId}, 10)")
    void insertPaperQuestion(@Param("paperId") Long paperId, @Param("questionId") Long questionId);

    @Select("SELECT p.id, p.title, p.category, p.total_score as totalScore, p.create_time as createTime, " +
            "(SELECT COUNT(*) FROM sys_paper_question pq WHERE pq.paper_id = p.id) as questionCount " +
            "FROM sys_paper p WHERE p.teacher_id = #{teacherId} ORDER BY p.create_time DESC")
    List<Map<String, Object>> listMyPapers(Long teacherId);

    @Insert("INSERT INTO sys_assignment(teacher_id, paper_id, publish_name, start_time, end_time, time_limit_minutes) " +
            "VALUES(#{teacherId}, #{paperId}, #{publishName}, #{startTime}, #{endTime}, #{timeLimitMinutes})")
    void insertAssignment(Map<String, Object> assignment);

    @Select("SELECT a.id, a.publish_name as publishName, a.start_time as startTime, a.end_time as endTime, a.time_limit_minutes as timeLimitMinutes, p.title as paperTitle " +
            "FROM sys_assignment a LEFT JOIN sys_paper p ON a.paper_id = p.id " +
            "WHERE a.teacher_id = #{teacherId} ORDER BY a.create_time DESC")
    List<Map<String, Object>> listMyAssignments(Long teacherId);

    @Select("SELECT id, title, category, total_score AS totalScore, create_time AS createTime, " +
            "(SELECT COUNT(*) FROM sys_paper_question pq WHERE pq.paper_id = p.id) AS questionCount " +
            "FROM sys_paper p WHERE p.id = #{paperId} AND p.teacher_id = #{teacherId}")
    Map<String, Object> getPaperForTeacher(@Param("paperId") Long paperId, @Param("teacherId") Long teacherId);

    @Select("SELECT q.id, q.type, q.category, q.content, CAST(q.options AS CHAR CHARACTER SET utf8mb4) AS options, " +
            "q.standard_answer AS standardAnswer, pq.score " +
            "FROM sys_paper_question pq JOIN sys_question q ON q.id = pq.question_id " +
            "WHERE pq.paper_id = #{paperId} ORDER BY pq.id")
    List<Map<String, Object>> listPaperQuestions(@Param("paperId") Long paperId);

    @Select("SELECT COUNT(1) FROM sys_assignment WHERE paper_id = #{paperId}")
    int countAssignmentsForPaper(@Param("paperId") Long paperId);

    @Delete("DELETE FROM sys_paper_question WHERE paper_id = #{paperId}")
    int deletePaperQuestions(@Param("paperId") Long paperId);

    @Delete("DELETE FROM sys_paper WHERE id = #{paperId} AND teacher_id = #{teacherId}")
    int deletePaper(@Param("paperId") Long paperId, @Param("teacherId") Long teacherId);

    @Select("SELECT id AS assignmentId, teacher_id AS teacherId, paper_id AS paperId, publish_name AS publishName, " +
            "start_time AS startTime, end_time AS endTime, time_limit_minutes AS timeLimitMinutes " +
            "FROM sys_assignment WHERE id = #{assignmentId} AND teacher_id = #{teacherId}")
    Map<String, Object> getAssignmentForTeacher(@Param("assignmentId") Long assignmentId, @Param("teacherId") Long teacherId);

    @Delete("DELETE FROM sys_student_record WHERE assignment_id = #{assignmentId}")
    int deleteStudentRecordsForAssignment(@Param("assignmentId") Long assignmentId);

    @Delete("DELETE FROM sys_assignment WHERE id = #{assignmentId} AND teacher_id = #{teacherId}")
    int deleteAssignment(@Param("assignmentId") Long assignmentId, @Param("teacherId") Long teacherId);

    @Select("SELECT COUNT(1) FROM sys_user WHERE teacher_id = #{teacherId} AND role = 'STUDENT'")
    int countStudentsInClass(@Param("teacherId") Long teacherId);

    @Select("SELECT r.student_id AS studentId, u.username, u.nickname, r.status, r.score, " +
            "CAST(r.student_answers AS CHAR CHARACTER SET utf8mb4) AS answersJson, r.submit_time AS submitTime " +
            "FROM sys_student_record r JOIN sys_user u ON u.id = r.student_id " +
            "WHERE r.assignment_id = #{assignmentId}")
    List<Map<String, Object>> listRecordsForAssignment(@Param("assignmentId") Long assignmentId);
}