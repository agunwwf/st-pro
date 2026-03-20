package com.example.admin.controller;

import com.example.admin.entity.ForumPost;
import com.example.admin.mapper.ForumMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum")
@CrossOrigin
public class ForumController {

    @Autowired
    private ForumMapper forumMapper;

    // 1. 获取所有帖子
    @GetMapping("/posts")
    public Result getPosts() {
        List<ForumPost> list = forumMapper.getList();
        return Result.success(list);
    }

    // 2. 发帖
    @PostMapping("/posts")
    public Result createPost(@RequestBody ForumPost post) {
        if (post.getUserId() == null) {
            return Result.error("用户ID不能为空");
        }
        forumMapper.insert(post);

        // 实时广播：通知所有人有新帖子
        try {
            com.example.admin.config.ForumHandler.broadcast("{\"type\":\"new_post\"}");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.success(post);
    }

    // 3. 互动操作 (点赞/收藏)
    @PostMapping("/action")
    public Result handleAction(@RequestBody java.util.Map<String, Object> params) {
        Object postIdObj = params.get("postId");
        Object userIdObj = params.get("userId");
        Object typeObj = params.get("type");
        if (postIdObj == null || userIdObj == null || typeObj == null) {
            return Result.error("postId, userId, type are required");
        }
        // 兼容前端传数字/字符串两种情况
        Long postId = (postIdObj instanceof Number)
                ? ((Number) postIdObj).longValue()
                : Long.valueOf(String.valueOf(postIdObj));
        Long userId = (userIdObj instanceof Number)
                ? ((Number) userIdObj).longValue()
                : Long.valueOf(String.valueOf(userIdObj));
        String type = String.valueOf(typeObj); // "vote" or "star"
        String actionType = type; // sys_forum_action.action_type: vote/star

        int existed = forumMapper.countAction(userId, postId, actionType);

        if ("vote".equals(type)) {
            if (existed > 0) {
                // 已点过赞 -> 取消点赞
                forumMapper.deleteAction(userId, postId, actionType);
                forumMapper.updateVotes(postId, -1);
            } else {
                // 未点过赞 -> 点赞
                forumMapper.insertAction(userId, postId, actionType);
                forumMapper.updateVotes(postId, 1);
            }
        } else if ("star".equals(type)) {
            if (existed > 0) {
                // 已收藏 -> 取消收藏
                forumMapper.deleteAction(userId, postId, actionType);
                forumMapper.updateStars(postId, -1);
            } else {
                // 未收藏 -> 收藏
                forumMapper.insertAction(userId, postId, actionType);
                forumMapper.updateStars(postId, 1);
            }
        } else {
            return Result.error("Invalid action type");
        }

        // 实时广播：通知所有人更新状态
        try {
            com.example.admin.config.ForumHandler.broadcast("{\"type\":\"post_update\", \"postId\":" + postId + "}");
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Result.success("操作成功");
    }
}
