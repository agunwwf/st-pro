package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.TeacherStudentBlockMapper;
import com.example.admin.mapper.TeacherStudentRequestMapper;
import com.example.admin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/student/teacher")
public class StudentTeacherController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private TeacherStudentRequestMapper requestMapper;

    @Autowired
    private TeacherStudentBlockMapper blockMapper;

    private Long uid(HttpServletRequest req) {
        Object v = req.getAttribute("userId");
        return v == null ? null : Long.valueOf(v.toString());
    }

    private boolean isStudent(HttpServletRequest req) {
        Long id = uid(req);
        if (id == null) return false;
        User u = userMapper.getById(id);
        return u != null && "STUDENT".equalsIgnoreCase(u.getRole());
    }

    @GetMapping("/current")
    public Result<Map<String, Object>> current(HttpServletRequest req) {
        if (!isStudent(req)) return Result.error("未授权访问");
        Long studentId = uid(req);
        Long teacherId = userMapper.getTeacherIdByUserId(studentId);
        Map<String, Object> data = new HashMap<>();
        data.put("teacherId", teacherId);
        return Result.success(data);
    }

    @PostMapping("/request-bind")
    public Result<Map<String, Object>> requestBind(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        if (!isStudent(req)) return Result.error("未授权访问");
        Long studentId = uid(req);
        Long teacherId = body.get("teacherId") == null ? null : Long.valueOf(body.get("teacherId").toString());
        if (teacherId == null) return Result.error("缺少 teacherId");

        User teacher = userMapper.getById(teacherId);
        if (teacher == null || !"ADMIN".equalsIgnoreCase(teacher.getRole())) return Result.error("老师不存在");

        Long curTeacherId = userMapper.getTeacherIdByUserId(studentId);
        if (curTeacherId != null) return Result.error("已绑定导师，请使用换导师");

        Integer active = blockMapper.getActive(teacherId, studentId);
        if (active != null && active == 1) return Result.error("你已被该老师踢出，无法再次申请加入");

        if (requestMapper.countPending(studentId, teacherId, "BIND") > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("ok", true);
            data.put("status", "PENDING");
            return Result.success(data);
        }

        Map<String, Object> row = new HashMap<>();
        row.put("studentId", studentId);
        row.put("oldTeacherId", null);
        row.put("newTeacherId", teacherId);
        row.put("reqType", "BIND");
        row.put("approveOld", 0);
        row.put("approveNew", 0);
        requestMapper.insert(row);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("requestId", row.get("id"));
        data.put("status", "PENDING");
        return Result.success(data);
    }

    @PostMapping("/request-switch")
    public Result<Map<String, Object>> requestSwitch(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        if (!isStudent(req)) return Result.error("未授权访问");
        Long studentId = uid(req);
        Long newTeacherId = body.get("newTeacherId") == null ? null : Long.valueOf(body.get("newTeacherId").toString());
        if (newTeacherId == null) return Result.error("缺少 newTeacherId");

        User newTeacher = userMapper.getById(newTeacherId);
        if (newTeacher == null || !"ADMIN".equalsIgnoreCase(newTeacher.getRole())) return Result.error("新老师不存在");

        Long oldTeacherId = userMapper.getTeacherIdByUserId(studentId);
        if (oldTeacherId == null) return Result.error("当前未绑定导师，无法换导师");
        if (oldTeacherId.equals(newTeacherId)) return Result.error("新老师不能与当前老师相同");

        Integer active = blockMapper.getActive(newTeacherId, studentId);
        if (active != null && active == 1) return Result.error("你已被该老师踢出，无法再次申请加入");

        if (requestMapper.countPending(studentId, newTeacherId, "SWITCH") > 0) {
            Map<String, Object> data = new HashMap<>();
            data.put("ok", true);
            data.put("status", "PENDING");
            return Result.success(data);
        }

        Map<String, Object> row = new HashMap<>();
        row.put("studentId", studentId);
        row.put("oldTeacherId", oldTeacherId);
        row.put("newTeacherId", newTeacherId);
        row.put("reqType", "SWITCH");
        row.put("approveOld", 0);
        row.put("approveNew", 0);
        requestMapper.insert(row);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("requestId", row.get("id"));
        data.put("status", "PENDING");
        return Result.success(data);
    }
}

