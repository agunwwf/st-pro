package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.TeacherStudentBlockMapper;
import com.example.admin.mapper.TeacherStudentRequestMapper;
import com.example.admin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher/class")
public class TeacherClassController {

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

    private boolean isAdmin(HttpServletRequest req) {
        Long id = uid(req);
        if (id == null) return false;
        User u = userMapper.getById(id);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    @GetMapping("/requests")
    public Result<List<Map<String, Object>>> listRequests(HttpServletRequest req, @RequestParam(defaultValue = "PENDING") String status) {
        if (!isAdmin(req)) return Result.error("未授权访问");
        Long teacherId = uid(req);
        return Result.success(requestMapper.listForTeacher(teacherId, status));
    }

    @PostMapping("/requests/{id}/approve")
    @Transactional
    public Result<Map<String, Object>> approve(HttpServletRequest req, @PathVariable Long id) {
        if (!isAdmin(req)) return Result.error("未授权访问");
        Long teacherId = uid(req);

        Map<String, Object> row = requestMapper.getById(id);
        if (row == null) return Result.error("申请不存在");
        if (!"PENDING".equals(String.valueOf(row.get("status")))) return Result.error("申请已处理");

        Long oldTeacherId = row.get("old_teacher_id") == null ? null : Long.valueOf(row.get("old_teacher_id").toString());
        Long newTeacherId = row.get("new_teacher_id") == null ? null : Long.valueOf(row.get("new_teacher_id").toString());
        if (!(teacherId.equals(oldTeacherId) || teacherId.equals(newTeacherId))) return Result.error("无权审批该申请");

        requestMapper.approveByTeacher(id, teacherId);
        Map<String, Object> updated = requestMapper.getById(id);
        if (updated == null) return Result.error("申请不存在");

        int approveOld = updated.get("approve_old") == null ? 0 : Integer.parseInt(updated.get("approve_old").toString());
        int approveNew = updated.get("approve_new") == null ? 0 : Integer.parseInt(updated.get("approve_new").toString());
        String reqType = String.valueOf(updated.get("req_type"));
        Long studentId = Long.valueOf(updated.get("student_id").toString());

        boolean shouldFinalize = false;
        if ("BIND".equalsIgnoreCase(reqType)) {
            shouldFinalize = approveNew == 1;
        } else if ("SWITCH".equalsIgnoreCase(reqType)) {
            shouldFinalize = approveOld == 1 && approveNew == 1;
        }

        if (shouldFinalize) {
            userMapper.setTeacherId(studentId, newTeacherId);
            requestMapper.markApproved(id);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        data.put("finalized", shouldFinalize);
        return Result.success(data);
    }

    @PostMapping("/requests/{id}/reject")
    public Result<Map<String, Object>> reject(HttpServletRequest req, @PathVariable Long id) {
        try {
            if (!isAdmin(req)) return Result.error("未授权访问");
            Long teacherId = uid(req);
            if (teacherId == null) return Result.error("未授权访问");

            Map<String, Object> row = requestMapper.getById(id);
            if (row == null) return Result.error("申请不存在");
            if (!"PENDING".equals(String.valueOf(row.get("status")))) return Result.error("申请已处理");

            Long oldTeacherId = row.get("old_teacher_id") == null ? null : Long.valueOf(row.get("old_teacher_id").toString());
            Long newTeacherId = row.get("new_teacher_id") == null ? null : Long.valueOf(row.get("new_teacher_id").toString());
            if (!(teacherId.equals(oldTeacherId) || teacherId.equals(newTeacherId))) return Result.error("无权审批该申请");

            Long studentId = row.get("student_id") == null ? null : Long.valueOf(row.get("student_id").toString());
            String reqType = row.get("req_type") == null ? "" : String.valueOf(row.get("req_type"));
            if (studentId != null && newTeacherId != null && !reqType.isBlank()) {
                // 避免唯一索引 (student_id,new_teacher_id,req_type,status) 在 REJECTED 状态冲突
                requestMapper.deleteDuplicateRejected(id, studentId, newTeacherId, reqType);
            }
            int affected = requestMapper.reject(id);
            if (affected <= 0) return Result.error("申请状态已变化，请刷新后重试");
            Map<String, Object> data = new HashMap<>();
            data.put("ok", true);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("拒绝失败，请刷新后重试");
        }
    }

    @PostMapping("/students/add")
    public Result<Map<String, Object>> addStudent(HttpServletRequest req, @RequestBody Map<String, Object> body) {
        if (!isAdmin(req)) return Result.error("未授权访问");
        Long teacherId = uid(req);
        String username = body.get("username") == null ? null : body.get("username").toString();
        boolean force = body.get("force") != null && Boolean.parseBoolean(body.get("force").toString());
        if (username == null || username.trim().isEmpty()) return Result.error("缺少 username");

        User target = userMapper.getByUsername(username.trim());
        if (target == null || !"STUDENT".equalsIgnoreCase(target.getRole())) return Result.error("学生不存在");

        Integer active = blockMapper.getActive(teacherId, target.getId());
        if (active != null && active == 1 && !force) return Result.error("该学生已被你踢出，无法再次加入（需要强制添加）");
        if (active != null && active == 1 && force) blockMapper.deactivate(teacherId, target.getId());

        userMapper.setTeacherId(target.getId(), teacherId);
        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        return Result.success(data);
    }

    @PostMapping("/students/{studentId}/kick")
    @Transactional
    public Result<Map<String, Object>> kick(HttpServletRequest req, @PathVariable Long studentId, @RequestBody(required = false) Map<String, Object> body) {
        if (!isAdmin(req)) return Result.error("未授权访问");
        Long teacherId = uid(req);

        User target = userMapper.getById(studentId);
        if (target == null || !"STUDENT".equalsIgnoreCase(target.getRole())) return Result.error("学生不存在");
        if (target.getTeacherId() == null || !teacherId.equals(target.getTeacherId())) return Result.error("该学生不在你的班级");

        userMapper.clearTeacherId(studentId);
        String reason = body == null || body.get("reason") == null ? "kick" : body.get("reason").toString();
        Map<String, Object> row = new HashMap<>();
        row.put("teacherId", teacherId);
        row.put("studentId", studentId);
        row.put("reason", reason);
        row.put("createdBy", teacherId);
        blockMapper.upsertActive(row);

        Map<String, Object> data = new HashMap<>();
        data.put("ok", true);
        return Result.success(data);
    }
}

