package com.example.admin.controller;

import com.example.admin.entity.LearningThread;

import com.example.admin.mapper.LearningThreadMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/thread")
@CrossOrigin
public class LearningThreadController {
    @Autowired
    private LearningThreadMapper threadMapper;

    @PostMapping("/add")
    public Result add(@RequestBody LearningThread thread) {
        threadMapper.insert(thread);
        return Result.success("Progress updated");
    }

    @GetMapping("/list")
    public Result list(@RequestParam Long userId) {
        return Result.success(threadMapper.getByUserId(userId));
    }
}