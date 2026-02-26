package com.example.admin.service;

import com.example.admin.entity.User;

public interface UserService {
    User getByUsername(String username);
    void updateById(User user);
    void register(User user);
}