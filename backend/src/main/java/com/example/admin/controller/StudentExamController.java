package com.example.admin.controller;

import com.example.admin.mapper.StudentExamMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student/exam")
public class StudentExamController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private StudentExamMapper studentExamMapper;
    private Long getUserIdFromToken(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            return Long.valueOf(userIdObj.toString());
        }
        return null;
    }

    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getMyExams(
            HttpServletRequest request,
            @RequestParam(required = false) Long teacherId
    ) {
        Long studentId = getUserIdFromToken(request);
        if (studentId == null) return Result.error("登录状态失效");

        // 仅影响测验模块
        if (teacherId != null) {
            Long boundTeacherId = studentExamMapper.getStudentTeacherId(studentId);
            if (boundTeacherId == null || !teacherId.equals(boundTeacherId)) {
                return Result.error("无权查看该老师的测验任务");
            }
        }
        return Result.success(studentExamMapper.listMyExamsByTeacher(studentId, teacherId));
    }

    @GetMapping("/detail")
    public Result<Map<String, Object>> getExamPaperDetail(HttpServletRequest request, @RequestParam Long assignmentId) {
        Long studentId = getUserIdFromToken(request);
        if (studentId == null) return Result.error("登录状态失效");
        if (assignmentId == null) return Result.error("缺少考试任务ID");

        // 校验该考试任务是否属于当前学生（同一导师下发）
        if (studentExamMapper.canAccessAssignment(assignmentId, studentId) <= 0) {
            return Result.error("无权访问该考试任务");
        }

        // 未开始/已截止的任务不允许进入作答页
        Map<String, Object> window = studentExamMapper.getAssignmentWindow(assignmentId);
        if (window == null) return Result.error("考试不存在");
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Object startObj = window.get("startTime");
        Object endObj = window.get("endTime");
        if (startObj instanceof java.util.Date startDate) {
            LocalDateTime start = LocalDateTime.ofInstant(startDate.toInstant(), ZoneId.systemDefault());
            if (now.isBefore(start)) return Result.error("考试尚未开始");
        }
        if (endObj instanceof java.util.Date endDate) {
            LocalDateTime end = LocalDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault());
            if (now.isAfter(end)) return Result.error("考试已截止");
        }

        Map<String, Object> detail = studentExamMapper.getExamDetail(assignmentId);
        if (detail == null) return Result.error("考试不存在");

        List<Map<String, Object>> questions = studentExamMapper.listExamQuestions(assignmentId);
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("examInfo", detail);
        resultData.put("questions", questions);

        return Result.success(resultData);
    }

    @GetMapping("/report")
    public Result<Map<String, Object>> getExamReport(HttpServletRequest request, @RequestParam Long assignmentId) {
        Long studentId = getUserIdFromToken(request);

        if (studentExamMapper.canAccessAssignment(assignmentId, studentId) <= 0) {
            return Result.error("无权查看该考试");
        }

        Map<String, Object> record = studentExamMapper.getStudentRecord(assignmentId, studentId);
        if (record == null) return Result.error("暂无答题记录");
        Integer status = record.get("status") == null ? null : Integer.parseInt(record.get("status").toString());
        if (status == null || status < 1) return Result.error("尚未交卷，无法查看解析");

        Map<String, Object> examInfo = studentExamMapper.getExamDetail(assignmentId);
        if (examInfo == null) return Result.error("考试不存在");

        List<Map<String, Object>> questions = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);

        Map<String, Object> data = new HashMap<>();
        data.put("examInfo", examInfo);
        data.put("record", record);
        data.put("questions", questions);
        return Result.success(data);
    }

    @PostMapping("/submit")
    public Result<Object> submitExam(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        Long studentId = getUserIdFromToken(request);
        if (studentId == null) return Result.error("越权操作！");

        Long assignmentId = Long.valueOf(req.get("assignmentId").toString());
        String answersJson = req.get("answers").toString();

        // 交卷校验归属
        if (studentExamMapper.canAccessAssignment(assignmentId, studentId) <= 0) {
            return Result.error("无权提交该考试任务");
        }

        // 时间窗口校验：截止后禁止交卷（避免刷接口强行提交）
        Map<String, Object> window = studentExamMapper.getAssignmentWindow(assignmentId);
        if (window == null) return Result.error("考试不存在");
        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        Object endObj = window.get("endTime");
        if (endObj instanceof java.util.Date endDate) {
            LocalDateTime end = LocalDateTime.ofInstant(endDate.toInstant(), ZoneId.systemDefault());
            if (now.isAfter(end)) return Result.error("考试已截止，无法提交");
        }

        // 防重复提交：已交卷/待批/已批都不允许覆盖答案
        Integer status = studentExamMapper.getStudentRecordStatus(assignmentId, studentId);
        if (status != null && status >= 1) {
            return Result.error("已提交，禁止重复交卷");
        }

        Integer score = calcScore(assignmentId, answersJson);
        studentExamMapper.submitExamRecord(assignmentId, studentId, answersJson, score);
        return Result.success((Object) null);
    }

    private Integer calcScore(Long assignmentId, String answersJson) {
        JsonNode answerRoot;
        try {
            answerRoot = OBJECT_MAPPER.readTree(answersJson == null ? "{}" : answersJson);
        } catch (Exception e) {
            answerRoot = OBJECT_MAPPER.createObjectNode();
        }

        int total = 0;
        List<Map<String, Object>> questions = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);
        for (Map<String, Object> q : questions) {
            Object idObj = q.get("id");
            if (idObj == null) continue;
            String qid = String.valueOf(idObj);
            String standard = q.get("standardAnswer") == null ? "" : String.valueOf(q.get("standardAnswer"));
            int score = q.get("score") == null ? 0 : Integer.parseInt(String.valueOf(q.get("score")));

            JsonNode ansNode = answerRoot.get(qid);
            String studentAns = ansNode == null || ansNode.isNull() ? "" : ansNode.asText("");
            if (answerMatched(studentAns, standard)) {
                total += score;
            }
        }
        return total;
    }

    private boolean answerMatched(String studentAns, String standardAns) {
        String s = normalize(studentAns);
        String t = normalize(standardAns);
        if (s.isEmpty() || t.isEmpty()) return false;

        // 标准答案可能写成 A/B、A|B、A或B，任一命中即算对
        String[] candidates = t.split("\\\\|/|或");
        for (String c : candidates) {
            if (s.equals(normalize(c))) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String v) {
        if (v == null) return "";
        return v.trim().replaceAll("\\s+", "").toUpperCase();
    }
}