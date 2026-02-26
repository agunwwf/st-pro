package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.UserMapper;
import com.example.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/user")
@CrossOrigin // 允许跨域
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    @PostMapping("/login")
    public Result login(@RequestBody User user) {
        // 简单模拟登录，实际需校验密码加密
        User dbUser = userService.getByUsername(user.getUsername());
        if (dbUser != null && dbUser.getPassword().equals(user.getPassword())) {
            return Result.success(dbUser);
        }
        return Result.error("Invalid credentials");
    }

    @PostMapping("/register")
    public Result register(@RequestBody User user) {
        User existing = userService.getByUsername(user.getUsername());
        if (existing != null) {
            return Result.error("Username already exists");
        }
        userService.register(user);
        return Result.success("Registration successful");
    }

    @PostMapping("/update")
    public Result updateProfile(@RequestBody User user) {
        userService.updateById(user);
        return Result.success("Profile updated");
    }

    @GetMapping("/search")
    public Result<List<User>> search(@RequestParam("keyword") String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(List.of());
        }
        return Result.success(userMapper.search(keyword.trim()));
    }
}