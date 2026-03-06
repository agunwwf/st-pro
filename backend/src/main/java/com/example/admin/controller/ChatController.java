package com.example.admin.controller;

import com.example.admin.entity.Friend;
import com.example.admin.entity.Message;
import com.example.admin.entity.User;
import com.example.admin.mapper.FriendMapper;
import com.example.admin.mapper.MessageMapper;
import com.example.admin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin
public class ChatController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private FriendMapper friendMapper;
    @Autowired
    private MessageMapper messageMapper;

    // 1. 搜索用户（支持模糊搜索）
    @GetMapping("/search")
    public Result search(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("Keyword cannot be empty");
        }

        // 模糊搜索用户名或昵称
        List<User> users = userMapper.searchUsers(keyword);

        if (users == null || users.isEmpty()) {
            return Result.error("User not found");
        }

        // 隐藏敏感信息
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }

    // 2. 发送好友申请
    @PostMapping("/friend/request")
    public Result sendRequest(@RequestBody Friend friend) {
        // 检查目标用户是否存在
        User target = userMapper.getByUsername(friend.getFriendUsername());
        if (target == null) return Result.error("User not found");

        friend.setFriendId(target.getId());

        // 检查是否重复
        if (friendMapper.checkRelation(friend.getUserId(), friend.getFriendId()) > 0) {
            return Result.error("Request already sent or already friends");
        }

        friendMapper.insertRequest(friend);
        return Result.success("Request sent");
    }

    // 3. 获取好友申请列表
    @GetMapping("/friend/requests")
    public Result getRequests(@RequestParam Long userId) {
        return Result.success(friendMapper.getFriendRequests(userId));
    }

    // 4. 同意好友申请
    @PostMapping("/friend/accept")
    public Result acceptRequest(@RequestBody Friend friend) {
        if (friend.getUserId() == null || friend.getFriendId() == null) {
            return Result.error("userId and friendId are required");
        }

        // 1) 把任意方向存在的申请记录置为已同意
        friendMapper.acceptRequest(friend.getUserId(), friend.getFriendId());

        // 2) upsert 双向好友关系，确保双方列表都能查到
        Friend a2b = new Friend();
        a2b.setUserId(friend.getUserId());
        a2b.setFriendId(friend.getFriendId());
        friendMapper.addFriendDirect(a2b);

        Friend b2a = new Friend();
        b2a.setUserId(friend.getFriendId());
        b2a.setFriendId(friend.getUserId());
        friendMapper.addFriendDirect(b2a);

        return Result.success("Friend added");
    }

    // 5. 获取我的好友列表
    @GetMapping("/friends")
    public Result getFriends(@RequestParam Long userId) {
        return Result.success(friendMapper.getMyFriends(userId));
    }

    // 5.1 删除好友（双向）
    @PostMapping("/friend/delete")
    public Result deleteFriend(@RequestBody java.util.Map<String, Object> payload) {
        Object uObj = payload.get("userId");
        Object fObj = payload.get("friendId");
        if (uObj == null || fObj == null) {
            return Result.error("userId and friendId are required");
        }
        Long userId = Long.valueOf(uObj.toString());
        Long friendId = Long.valueOf(fObj.toString());
        friendMapper.deleteRelation(userId, friendId);
        return Result.success("Friend deleted");
    }

    // 6. 获取聊天记录
    @GetMapping("/messages")
    public Result getMessages(@RequestParam String user1, @RequestParam String user2) {
        return Result.success(messageMapper.getHistory(user1, user2));
    }

    // 7. 文件上传（图片/文件），供聊天使用
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        try {
            // 上传目录：项目根目录下的 uploads（使用绝对路径更稳妥）
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalName = file.getOriginalFilename();
            if (originalName == null) {
                originalName = "unknown";
            }

            String ext = "";
            int dot = originalName.lastIndexOf('.');
            if (dot >= 0) {
                ext = originalName.substring(dot);
            }

            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(filename);

            // 使用 NIO 拷贝，兼容性更好
            Files.copy(file.getInputStream(), target);

            // 返回给前端用于访问的 URL
            String url = "/uploads/" + filename;
            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("fileName", originalName);

            return Result.success(data);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
}