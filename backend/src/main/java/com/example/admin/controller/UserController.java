package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.UserMapper;
import com.example.admin.mapper.CheckInMapper;
import com.example.admin.service.UserService;
import com.example.admin.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CheckInMapper checkInMapper;

    @GetMapping("/me")
    public Result<User> getCurrentUser(HttpServletRequest req) {
        Long id = (Long) req.getAttribute("userId");
        User u = userService.getById(id);
        if (u == null) {
            return Result.error("用户不存在");
        }
        u.setPassword(null);
        u.setIpLocation(inferIpLocation(req));
        return Result.success(u);
    }

    private String inferIpLocation(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip != null && !ip.trim().isEmpty()) {
            ip = ip.split(",")[0].trim();
        } else {
            ip = req.getRemoteAddr();
        }
        if (ip == null) return "未知";
        if (ip.startsWith("127.") || ip.startsWith("10.") || ip.startsWith("192.168.")) return "内网";
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                if (second >= 16 && second <= 31) return "内网";
            } catch (Exception ignored) {}
        }
        return "公网";
    }

    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        User dbUser = userService.getByUsername(user.getUsername());
        if (dbUser == null || !dbUser.getPassword().equals(user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        if (user.getRole() != null && !dbUser.getRole().equals(user.getRole())) {
            return Result.error("角色权限不匹配");
        }
        String token =
                jwtUtil.generateToken(
                        dbUser.getId(),          // 来自 sys_user.id
                        dbUser.getUsername()
                );

        java.util.Map<String,Object> map =
                new java.util.HashMap<>();

        map.put("token",token);
        map.put("user",dbUser);   // 要返回的用户信息

        return Result.success(map);

    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        User existing = userService.getByUsername(user.getUsername());
        if (existing != null) return Result.error("用户名已存在");
        userService.register(user);
        return Result.success("注册成功");
    }

    @GetMapping("/students")
    public Result getStudents(HttpServletRequest req) {
        Long uid = (Long) req.getAttribute("userId");
        User u = uid == null ? null : userService.getById(uid);
        if (u == null || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return Result.error("未授权访问");
        }
        return Result.success(userService.getAllStudents());
    }

    @DeleteMapping("/student/{id}")
    public Result deleteStudent(HttpServletRequest req, @PathVariable Long id) {
        Long uid = (Long) req.getAttribute("userId");
        User u = uid == null ? null : userService.getById(uid);
        if (u == null || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return Result.error("未授权访问");
        }
        userService.deleteStudent(id);
        return Result.success("删除成功");
    }

    @PostMapping("/student/model")
    public Result setModel(HttpServletRequest req, @RequestBody java.util.Map<String, Object> params) {
        Long uid = (Long) req.getAttribute("userId");
        User u = uid == null ? null : userService.getById(uid);
        if (u == null || !"ADMIN".equalsIgnoreCase(u.getRole())) {
            return Result.error("未授权访问");
        }
        Long id = Long.valueOf(params.get("id").toString());
        Boolean isModel = (Boolean) params.get("isModel");
        userService.setModelStudent(id, isModel);
        return Result.success("设置成功");
    }

    @PostMapping("/checkin")
    public Result checkIn(HttpServletRequest req, @RequestBody java.util.Map<String, Object> params) {
        Long id = (Long) req.getAttribute("userId");
        if (params.containsKey("date")) {
            String date = params.get("date").toString();
            checkInMapper.insert(id, date);
            return Result.success("Check-in successful");
        }
        if (params.containsKey("count")) {
            Integer count = params.get("count") instanceof Number
                    ? ((Number) params.get("count")).intValue() : Integer.parseInt(params.get("count").toString());
            userService.checkIn(id, count);
            return Result.success("打卡成功");
        }
        return Result.error("参数错误");
    }

    @GetMapping("/checkin/dates")
    public Result<List<String>> getCheckInDates(HttpServletRequest req, @RequestParam(required = false) Long userId) {
        Long id = userId != null ? userId : (Long) req.getAttribute("userId");
        if (!id.equals(req.getAttribute("userId"))) {
            return Result.error("无权查询他人打卡记录");
        }
        return Result.success(checkInMapper.getCheckInDates(id));
    }

    @GetMapping("/streak")
    public Result<Integer> getStreak(HttpServletRequest req, @RequestParam(required = false) Long userId) {
        Long id = userId != null ? userId : (Long) req.getAttribute("userId");
        if (!id.equals(req.getAttribute("userId"))) {
            return Result.error("无权查询他人打卡记录");
        }
        try {
            List<String> dates = checkInMapper.getCheckInDates(id);
            if (dates == null || dates.isEmpty()) return Result.success(0);

            int streak = 0;
            java.time.LocalDate today = java.time.LocalDate.now();
            java.time.LocalDate current = today;

            // Check if checked in today or yesterday to start streak
            boolean checkedInToday = dates.contains(today.toString());
            boolean checkedInYesterday = dates.contains(today.minusDays(1).toString());

            if (!checkedInToday && !checkedInYesterday) return Result.success(0);

            if (!checkedInToday) current = today.minusDays(1);

            for (String dateStr : dates) {
                // Handle potential time part if any (though DATE_FORMAT should have removed it)
                String cleanDate = dateStr.split(" ")[0];
                java.time.LocalDate d = java.time.LocalDate.parse(cleanDate);

                if (d.equals(current)) {
                    streak++;
                    current = current.minusDays(1);
                } else if (d.isBefore(current)) {
                    break;
                }
            }
            return Result.success(streak);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.success(0); // Fallback to 0 instead of error
        }
    }

    @PostMapping("/update")
    public Result updateProfile(HttpServletRequest req, @RequestBody User user) {
        Long tokenUserId = (Long) req.getAttribute("userId");
        if (user.getId() == null || !user.getId().equals(tokenUserId)) {
            return Result.error("只能修改自己的资料");
        }
        userService.updateById(user);
        return Result.success("Profile updated");
    }

    @PostMapping("/account/change")
    public Result<Object> changeAccount(HttpServletRequest req, @RequestBody Map<String, String> body) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) return Result.error("请先登录");
        String newUsername = body.get("newUsername") == null ? "" : body.get("newUsername").trim();
        if (newUsername.isEmpty()) return Result.error("新账号不能为空");

        User me = userService.getById(userId);
        if (me == null) return Result.error("用户不存在");
        if (newUsername.equals(me.getUsername())) return Result.error("新账号不能与当前账号一致");
        if (userMapper.countUsernameExistsExcludeId(userId, newUsername) > 0) return Result.error("该账号已存在");

        String ym = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        Integer changed = userMapper.getUsernameChangeCount(userId, ym);
        int count = changed == null ? 0 : changed;
        if (count >= 5) return Result.error("本月账号修改次数已达上限（5次）");

        userMapper.updateUsernameById(userId, newUsername);
        userMapper.increaseUsernameChangeCount(userId, ym);
        return Result.success("账号修改成功");
    }

    @PostMapping("/password/change")
    public Result<Object> changePassword(HttpServletRequest req, @RequestBody Map<String, String> body) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) return Result.error("请先登录");
        String oldPassword = body.get("oldPassword") == null ? "" : body.get("oldPassword");
        String newPassword = body.get("newPassword") == null ? "" : body.get("newPassword");
        String confirmPassword = body.get("confirmPassword") == null ? "" : body.get("confirmPassword");

        if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
            return Result.error("请完整填写密码信息");
        }
        if (newPassword.length() < 6 || newPassword.length() > 12) {
            return Result.error("新密码必须为6-12位");
        }
        if (!newPassword.equals(confirmPassword)) {
            return Result.error("两次输入的新密码不一致");
        }
        User me = userService.getById(userId);
        if (me == null) return Result.error("用户不存在");
        if (!oldPassword.equals(me.getPassword())) {
            return Result.error("旧密码错误");
        }
        if (oldPassword.equals(newPassword)) {
            return Result.error("新密码不能与旧密码相同");
        }

        userMapper.updatePasswordById(userId, newPassword);
        return Result.success("密码修改成功");
    }

    @PostMapping("/password/verify")
    public Result<Object> verifyOldPassword(HttpServletRequest req, @RequestBody Map<String, String> body) {
        Long userId = (Long) req.getAttribute("userId");
        if (userId == null) return Result.error("请先登录");
        String oldPassword = body.get("oldPassword") == null ? "" : body.get("oldPassword");
        if (oldPassword.isEmpty()) return Result.error("请输入旧密码");
        User me = userService.getById(userId);
        if (me == null) return Result.error("用户不存在");
        if (!oldPassword.equals(me.getPassword())) return Result.error("旧密码错误");
        return Result.success("校验通过");
    }

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Result.success(List.of());
        return Result.success(userMapper.searchUsers(keyword.trim()));
    }
}