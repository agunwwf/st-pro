package com.example.admin.controller;

import com.example.admin.entity.LearningThread;
import com.example.admin.mapper.LearningThreadMapper;
import com.example.admin.mapper.StudentExamMapper;
import com.example.admin.mapper.TeacherAdminMapper;
import com.example.admin.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/thread")
@CrossOrigin
public class LearningThreadController {
    @Autowired
    private LearningThreadMapper threadMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StudentExamMapper studentExamMapper;
    @Autowired
    private TeacherAdminMapper teacherAdminMapper;

    @PostMapping("/add")
    public Result add(@RequestBody LearningThread thread) {
        threadMapper.insert(thread);
        return Result.success("Progress updated");
    }

    @GetMapping("/list")
    public Result list(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error("登录状态失效");
        }
        return Result.success(threadMapper.getByUserId(userId));
    }

    @GetMapping("/calendar/student")
    public Result<Map<String, Object>> studentCalendar(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return Result.error("登录状态失效");
        List<LearningThread> activities = threadMapper.getByUserId(userId);
        List<Map<String, Object>> exams = studentExamMapper.listMyExamsByTeacher(userId, null);
        return Result.success(Map.of(
                "activities", activities,
                "examTasks", exams
        ));
    }

    @GetMapping("/calendar/teacher")
    public Result<Map<String, Object>> teacherCalendar(HttpServletRequest request,
                                                        @RequestParam(defaultValue = "300") Integer limit) {
        Long teacherId = (Long) request.getAttribute("userId");
        if (teacherId == null) return Result.error("登录状态失效");
        com.example.admin.entity.User me = userMapper.getById(teacherId);
        if (me == null || !"ADMIN".equalsIgnoreCase(me.getRole())) {
            return Result.error("未授权访问");
        }
        int safeLimit = Math.max(50, Math.min(limit == null ? 300 : limit, 1000));
        List<Map<String, Object>> classActivities = threadMapper.listClassActivities(teacherId, safeLimit);
        List<Map<String, Object>> assignments = teacherAdminMapper.listMyAssignments(teacherId);
        long todayActiveStudents = classActivities.stream()
                .filter(x -> {
                    Object ct = x.get("createTime");
                    return ct != null && String.valueOf(ct).startsWith(LocalDate.now().toString());
                })
                .map(x -> String.valueOf(x.get("userId")))
                .collect(Collectors.toSet())
                .size();
        return Result.success(Map.of(
                "classActivities", classActivities,
                "assignments", assignments,
                "todayActiveStudents", todayActiveStudents
        ));
    }
}