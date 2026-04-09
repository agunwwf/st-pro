package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.StudentExamMapper;
import com.example.admin.mapper.TeacherAdminMapper;
import com.example.admin.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherAdminController {

    @Autowired
    private TeacherAdminMapper teacherAdminMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentExamMapper studentExamMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Long getUserIdFromToken(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            return Long.valueOf(userIdObj.toString());
        }
        return null;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Long uid = getUserIdFromToken(request);
        if (uid == null) return false;
        User u = userMapper.getById(uid);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    @GetMapping("/list-teachers")
    public Result<List<Map<String, Object>>> listTeachers() {
        return Result.success(teacherAdminMapper.listAllTeachers());
    }

    @GetMapping("/my-students")
    public Result<List<Map<String, Object>>> myStudents(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        return Result.success(teacherAdminMapper.listMyStudents(teacherId));
    }

    @GetMapping("/lms/questions")
    public Result<List<Map<String, Object>>> getQuestions(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");

        return Result.success(teacherAdminMapper.listQuestionsPaged(0, 50, null, null, null));
    }

    @GetMapping("/lms/questions/page")
    public Result<Map<String, Object>> getQuestionsPaged(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 200) size = 200; // 防被恶意拉爆

        int offset = (page - 1) * size;
        long total = teacherAdminMapper.countQuestions(category, type, keyword);
        List<Map<String, Object>> items = teacherAdminMapper.listQuestionsPaged(offset, size, category, type, keyword);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("page", page);
        data.put("size", size);
        data.put("total", total);
        data.put("items", items);
        return Result.success(data);
    }

    @GetMapping("/lms/papers")
    public Result<List<Map<String, Object>>> getPapers(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        return Result.success(teacherAdminMapper.listMyPapers(teacherId));
    }

    @PostMapping("/lms/paper/create")
    @Transactional
    public Result<Object> createPaper(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        req.put("teacherId", teacherId);
        teacherAdminMapper.insertPaper(req);

        Long paperId = Long.valueOf(req.get("id").toString());
        List<Integer> questionIds = (List<Integer>) req.get("questionIds");
        for (Integer qId : questionIds) {
            teacherAdminMapper.insertPaperQuestion(paperId, qId.longValue());
        }
        return Result.success((Object) null);
    }

    @PostMapping("/lms/assignment/publish")
    public Result<Object> publishAssignment(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        req.put("teacherId", teacherId);
        teacherAdminMapper.insertAssignment(req);
        return Result.success((Object) null);
    }

    @GetMapping("/lms/assignments")
    public Result<List<Map<String, Object>>> getAssignments(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        return Result.success(teacherAdminMapper.listMyAssignments(teacherId));
    }

    /** 查看试卷模板详情（含题目与标准答案），用于教师自查组卷 */
    @GetMapping("/lms/paper/{paperId}/detail")
    public Result<Map<String, Object>> paperDetail(HttpServletRequest request, @PathVariable Long paperId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        Map<String, Object> paper = teacherAdminMapper.getPaperForTeacher(paperId, teacherId);
        if (paper == null) return Result.error("试卷不存在");
        List<Map<String, Object>> questions = teacherAdminMapper.listPaperQuestions(paperId);
        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", questions);
        return Result.success(data);
    }

    /** 删除试卷模板：若已被考试任务引用则禁止删除 */
    @DeleteMapping("/lms/paper/{paperId}")
    @Transactional
    public Result<Object> deletePaper(HttpServletRequest request, @PathVariable Long paperId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        if (teacherAdminMapper.getPaperForTeacher(paperId, teacherId) == null) {
            return Result.error("试卷不存在");
        }
        if (teacherAdminMapper.countAssignmentsForPaper(paperId) > 0) {
            return Result.error("该模板已被考试任务使用，请先删除对应考试任务");
        }
        teacherAdminMapper.deletePaperQuestions(paperId);
        teacherAdminMapper.deletePaper(paperId, teacherId);
        return Result.success((Object) null);
    }

    /** 删除已发布的考试任务（同时删除学生答卷记录） */
    @DeleteMapping("/lms/assignment/{assignmentId}")
    @Transactional
    public Result<Object> deleteAssignment(HttpServletRequest request, @PathVariable Long assignmentId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        if (teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId) == null) {
            return Result.error("考试任务不存在");
        }
        teacherAdminMapper.deleteStudentRecordsForAssignment(assignmentId);
        teacherAdminMapper.deleteAssignment(assignmentId, teacherId);
        return Result.success((Object) null);
    }

    /**
     * 考试截止后的数据分析：班级参与、均分、每题正确率与未作答人数等。
     * 未截止返回错误文案，前端提示即可。
     */
    @GetMapping("/lms/assignment/{assignmentId}/analytics")
    public Result<Map<String, Object>> assignmentAnalytics(HttpServletRequest request, @PathVariable Long assignmentId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        Map<String, Object> assignRow = teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId);
        if (assignRow == null) return Result.error("考试任务不存在");
        Map<String, Object> assign = new HashMap<>(assignRow);
        Map<String, Object> examTitleRow = studentExamMapper.getExamDetail(assignmentId);
        if (examTitleRow != null && examTitleRow.get("paperTitle") != null) {
            assign.put("paperTitle", examTitleRow.get("paperTitle"));
        }

        LocalDateTime end = toLocalDateTime(assign.get("endTime"));
        if (end != null && LocalDateTime.now(ZoneId.systemDefault()).isBefore(end)) {
            return Result.error("考试尚未截止，截止后可查看数据分析");
        }

        int classSize = teacherAdminMapper.countStudentsInClass(teacherId);
        List<Map<String, Object>> rawRecords = teacherAdminMapper.listRecordsForAssignment(assignmentId);
        List<Map<String, Object>> submittedRows = new ArrayList<>();
        double scoreSum = 0.0;
        for (Map<String, Object> row : rawRecords) {
            int st = row.get("status") == null ? 0 : Integer.parseInt(row.get("status").toString());
            if (st >= 1) {
                submittedRows.add(row);
                if (row.get("score") != null) {
                    scoreSum += Double.parseDouble(row.get("score").toString());
                }
            }
        }
        int submittedCount = submittedRows.size();
        int notSubmittedCount = Math.max(0, classSize - submittedCount);
        BigDecimal avg = submittedCount > 0
                ? BigDecimal.valueOf(scoreSum / submittedCount).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Map<String, Object>> questions = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);
        List<Map<String, Object>> questionStats = new ArrayList<>();
        int idx = 1;
        for (Map<String, Object> q : questions) {
            Long qid = Long.valueOf(q.get("id").toString());
            String type = q.get("type") == null ? "" : q.get("type").toString();
            String std = q.get("standardAnswer") == null ? null : q.get("standardAnswer").toString();
            int correct = 0;
            int blank = 0;
            int wrong = 0;
            for (Map<String, Object> rec : submittedRows) {
                String json = rec.get("answersJson") == null ? null : rec.get("answersJson").toString();
                JsonNode root;
                try {
                    root = OBJECT_MAPPER.readTree(json == null || json.isBlank() ? "{}" : json);
                } catch (Exception e) {
                    root = OBJECT_MAPPER.createObjectNode();
                }
                String ans = readAnswerForQuestion(root, qid);
                if (ans.isEmpty()) {
                    blank++;
                } else if (answersMatch(type, ans, std)) {
                    correct++;
                } else {
                    wrong++;
                }
            }
            int base = submittedCount;
            BigDecimal rate = base > 0
                    ? BigDecimal.valueOf(100.0 * correct / base).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Map<String, Object> one = new HashMap<>(q);
            one.put("index", idx++);
            one.put("correctCount", correct);
            one.put("wrongCount", wrong);
            one.put("blankCount", blank);
            one.put("submittedCount", base);
            one.put("correctRate", rate.doubleValue());
            one.put("notSubmittedCount", notSubmittedCount);
            questionStats.add(one);
        }

        List<Map<String, Object>> studentRows = new ArrayList<>();
        for (Map<String, Object> row : rawRecords) {
            Map<String, Object> s = new HashMap<>();
            s.put("studentId", row.get("studentId"));
            s.put("username", row.get("username"));
            s.put("nickname", row.get("nickname"));
            s.put("status", row.get("status"));
            s.put("score", row.get("score"));
            s.put("submitTime", row.get("submitTime"));
            studentRows.add(s);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("classSize", classSize);
        summary.put("submittedCount", submittedCount);
        summary.put("notSubmittedCount", notSubmittedCount);
        summary.put("averageScore", avg.doubleValue());

        Map<String, Object> data = new HashMap<>();
        data.put("assignment", assign);
        data.put("summary", summary);
        data.put("students", studentRows);
        data.put("questions", questionStats);
        return Result.success(data);
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (o instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    private static String readAnswerForQuestion(JsonNode answersRoot, Long qid) {
        if (answersRoot == null || !answersRoot.isObject()) {
            return "";
        }
        JsonNode n = answersRoot.get(String.valueOf(qid));
        if (n == null || n.isNull()) {
            return "";
        }
        if (n.isTextual()) {
            return n.asText().trim();
        }
        if (n.isNumber()) {
            return n.asText().trim();
        }
        return n.toString().trim();
    }

    private static boolean answersMatch(String type, String studentAns, String standard) {
        if (standard == null) {
            return false;
        }
        String s = studentAns == null ? "" : studentAns.trim();
        if (s.isEmpty()) {
            return false;
        }
        return standard.trim().equalsIgnoreCase(s);
    }
}