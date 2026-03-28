package com.example.admin.service.impl;

import com.example.admin.entity.User;
import com.example.admin.mapper.UserMapper;
import com.example.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public User getById(Long id) {
        return userMapper.getById(id);
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.getByUsername(username);
    }

    @Override
    public void updateById(User user) {
        userMapper.updateById(user);
    }

    @Override
    public void register(User user) {
        // 设置默认角色
        if (user.getRole() == null) {
            user.setRole("STUDENT");
        }
        // 设置默认昵称和头像
        if (user.getNickname() == null || user.getNickname().trim().isEmpty()) {
            user.setNickname(user.getUsername());
        }
        if (user.getAvatar() == null) {
            user.setAvatar("https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png");
        }
        userMapper.insert(user);
    }

    @Override
    public List<User> getAllStudents() {
        return userMapper.getAllStudents();
    }

    @Override
    public void deleteStudent(Long id) {
        userMapper.deleteStudentById(id);
    }

    @Override
    public void setModelStudent(Long id, boolean isModel) {

        userMapper.updateModel(id, isModel ? 1 : 0);
    }

    @Override
    public void checkIn(Long id, Integer count) {
        if (count == null) {
            return;
        }
        userMapper.updateCheckInCount(id, count);
    }
}