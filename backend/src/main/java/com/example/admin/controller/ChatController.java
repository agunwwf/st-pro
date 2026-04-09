package com.example.admin.controller;

import com.example.admin.entity.Friend;
import com.example.admin.entity.Message;
import com.example.admin.entity.User;
import com.example.admin.mapper.FriendMapper;
import com.example.admin.mapper.MessageMapper;
import com.example.admin.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
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
    public Result getRequests(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
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
    public Result getFriends(HttpServletRequest request) {
        Long userId = Long.valueOf(request.getAttribute("userId").toString());
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
    public Result getMessages(@RequestParam String user1, @RequestParam String user2, HttpServletRequest request) {
        String me = (String) request.getAttribute("username");
        if (me == null || me.trim().isEmpty()) return Result.error("请先登录");
        // 只允许查询自己的会话，防止越权读取
        if (!me.equals(user1) && !me.equals(user2)) return Result.error("无权限查看该会话");
        String peer = me.equals(user1) ? user2 : user1;
        messageMapper.ensureClearLogTable();
        java.time.LocalDateTime clearTime = messageMapper.getClearTime(me, peer);
        if (clearTime != null) {
            return Result.success(messageMapper.getHistoryAfter(me, peer, clearTime));
        }
        return Result.success(messageMapper.getHistory(me, peer));
    }

    // 6.1 保存消息（落库），刷新后仍可读到历史记录
    @PostMapping("/message/save")
    public Result saveMessage(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String fromUsername = (String) request.getAttribute("username");
        if (fromUsername == null || fromUsername.trim().isEmpty()) {
            return Result.error("请先登录");
        }

        Object toObj = payload.get("toUser");
        Object contentObj = payload.get("content");
        Object msgTypeObj = payload.get("msgType");
        Object fileNameObj = payload.get("fileName");

        if (toObj == null || contentObj == null) {
            return Result.error("不能为空");
        }

        String toUser = toObj.toString();
        String content = contentObj.toString();
        String msgType = msgTypeObj == null ? "text" : msgTypeObj.toString();
        String fileName = fileNameObj == null ? null : fileNameObj.toString();

        if (toUser.trim().isEmpty() || content.trim().isEmpty()) {
            return Result.error("不能为空");
        }

        Message msg = new Message();
        msg.setFromUser(fromUsername.trim());
        msg.setToUser(toUser.trim());
        msg.setContent(content);
        msg.setMsgType(msgType);
        msg.setFileName(fileName);
        msg.setCreateTime(LocalDateTime.now());

        messageMapper.insert(msg);
        return Result.success("ok");
    }

    // 6.2 清空“我”的会话记录（不影响对方）
    @PostMapping("/messages/clear")
    public Result clearMyConversation(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String me = (String) request.getAttribute("username");
        if (me == null || me.trim().isEmpty()) return Result.error("请先登录");
        Object peerObj = payload.get("peerUsername");
        if (peerObj == null) return Result.error("peerUsername 不能为空");
        String peer = peerObj.toString().trim();
        if (peer.isEmpty()) return Result.error("peerUsername 不能为空");
        if (peer.equals(me)) return Result.error("不能清空自己的自聊会话");
        messageMapper.ensureClearLogTable();
        messageMapper.upsertClearTime(me, peer, LocalDateTime.now());
        return Result.success("已清空你与该用户的聊天记录（仅你可见）");
    }


    // 7. 文件上传（生成 API 访问链接，而不是物理静态链接）
    @PostMapping("/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("文件为空");
        }

        try {
          
            Path uploadDir = Paths.get(System.getProperty("user.dir"), "uploads");
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.lastIndexOf('.') >= 0
                    ? originalName.substring(originalName.lastIndexOf('.')) : "";

      
            String filename = UUID.randomUUID().toString().replace("-", "") + ext;
            Path target = uploadDir.resolve(filename);

            Files.copy(file.getInputStream(), target);

            //返回一个 API 接口地址
            String url = "/api/chat/file/" + filename;

            Map<String, String> data = new HashMap<>();
            data.put("url", url);
            data.put("fileName", originalName);

            return Result.success(data);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }
    // 8. 流式读取文件接口 (前端 <img> 标签的 src 就填这个接口)
    @GetMapping("/file/{fileName}")
    public void getFile(@PathVariable String fileName, jakarta.servlet.http.HttpServletResponse response) {
        Path filePath = Paths.get(System.getProperty("user.dir"), "uploads", fileName);

        // 如果文件不存在，返回 404
        if (!Files.exists(filePath)) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        try {
            // 自动推断文件类型（比如 image/png, application/pdf）
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream"; // 默认二进制流
            }
            response.setContentType(contentType);

            // 加上缓存控制，让浏览器缓存图片，不用每次打开聊天记录都重新下载，提升体验
            response.setHeader("Cache-Control", "max-age=864000");

            // 把硬盘里的文件变成流，直接塞进 HTTP 响应里冲给前端
            Files.copy(filePath, response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException e) {
            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}