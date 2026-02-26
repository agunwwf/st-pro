package com.example.admin.controller;

import com.example.admin.entity.Friend;
import com.example.admin.entity.Message;
import com.example.admin.entity.User;
import com.example.admin.mapper.FriendMapper;
import com.example.admin.mapper.MessageMapper;
import com.example.admin.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    // 1. 搜索用户（支持按 username 或 nickname 模糊匹配）
    @GetMapping("/search")
    public Result search(@RequestParam String keyword) {
        String kw = (keyword == null) ? "" : keyword.trim();
        if (kw.isEmpty()) return Result.error("用户不存在");

        List<User> list = userMapper.search(kw);
        if (list == null || list.isEmpty()) return Result.error("用户不存在");
        if (list.size() > 1) return Result.error("多个用户匹配，请输入精确账号或昵称");
        User user = list.get(0);
        user.setPassword(null);
        return Result.success(user);
    }

    // 2. 发送好友申请
    @PostMapping("/friend/request")
    public Result sendRequest(@RequestBody Friend friend) {
        // 检查目标用户是否存在
        User target = userMapper.getByUsername(friend.getFriendUsername());
        if (target == null) return Result.error("用户不存在");

        friend.setFriendId(target.getId());

        // 检查是否重复
        if (friendMapper.checkRelation(friend.getUserId(), friend.getFriendId()) > 0) {
            return Result.error("已发送申请或已是好友");
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
        // 更新对方的申请状态为1
        friendMapper.acceptRequest(friend.getUserId(), friend.getFriendId());
        // 为自己也插入一条记录（双向好友）
        Friend reverse = new Friend();
        reverse.setUserId(friend.getUserId());
        reverse.setFriendId(friend.getFriendId());
        friendMapper.addFriendDirect(reverse);

        return Result.success("Friend added");
    }

    // 5. 获取我的好友列表
    @GetMapping("/friends")
    public Result getFriends(@RequestParam Long userId) {
        return Result.success(friendMapper.getMyFriends(userId));
    }

    // 6. 获取聊天记录
    @GetMapping("/messages")
    public Result getMessages(@RequestParam String user1, @RequestParam String user2) {
        return Result.success(messageMapper.getHistory(user1, user2));
    }
}