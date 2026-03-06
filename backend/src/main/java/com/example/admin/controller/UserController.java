package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.UserMapper;
import com.example.admin.mapper.CheckInMapper;
import com.example.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CheckInMapper checkInMapper;

    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        User dbUser = userService.getByUsername(user.getUsername());
        if (dbUser == null || !dbUser.getPassword().equals(user.getPassword())) {
            return Result.error("用户名或密码错误");
        }
        if (user.getRole() != null && !dbUser.getRole().equals(user.getRole())) {
            return Result.error("角色权限不匹配");
        }
        return Result.success(dbUser);
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        User existing = userService.getByUsername(user.getUsername());
        if (existing != null) return Result.error("用户名已存在");
        userService.register(user);
        return Result.success("注册成功");
    }

    @GetMapping("/students")
    public Result getStudents() {
        return Result.success(userService.getAllStudents());
    }

    @DeleteMapping("/student/{id}")
    public Result deleteStudent(@PathVariable Long id) {
        userService.deleteStudent(id);
        return Result.success("删除成功");
    }

    @PostMapping("/student/model")
    public Result setModel(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.valueOf(params.get("id").toString());
        Boolean isModel = (Boolean) params.get("isModel");
        userService.setModelStudent(id, isModel);
        return Result.success("设置成功");
    }

    @PostMapping("/checkin")
    public Result checkIn(@RequestBody java.util.Map<String, Object> params) {
        Long id = Long.valueOf(params.get("userId").toString());
        String date = params.get("date").toString();
        checkInMapper.insert(id, date);
        return Result.success("Check-in successful");
    }

    @GetMapping("/checkin/dates")
    public Result<List<String>> getCheckInDates(@RequestParam Long userId) {
        return Result.success(checkInMapper.getCheckInDates(userId));
    }

    @GetMapping("/streak")
    public Result<Integer> getStreak(@RequestParam Long userId) {
        try {
            List<String> dates = checkInMapper.getCheckInDates(userId);
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
    public Result updateProfile(@RequestBody User user) {
        userService.updateById(user);
        return Result.success("Profile updated");
    }

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return Result.success(List.of());
        return Result.success(userMapper.searchUsers(keyword.trim()));
    }
}