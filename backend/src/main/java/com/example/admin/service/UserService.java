package com.example.admin.service;

import com.example.admin.entity.User;

import java.util.List;

public interface UserService {
    User getById(Long id);

    User getByUsername(String username);
    void updateById(User user);
    void register(User user);
    List<User> getAllStudents();
    void deleteStudent(Long id);
    void setModelStudent(Long id, boolean isModel);
    void checkIn(Long id, Integer count);
}