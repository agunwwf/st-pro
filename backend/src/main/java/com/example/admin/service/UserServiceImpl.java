package com.example.admin.service;

import com.example.admin.entity.User;
import com.example.admin.mapper.UserMapper;
import com.example.admin.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

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
        userMapper.insert(user);
    }
}