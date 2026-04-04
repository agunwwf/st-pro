package com.example.admin.controller;

import com.example.admin.entity.ForumComment;
import com.example.admin.entity.ForumPost;
import com.example.admin.mapper.ForumCommentMapper;
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

    @Autowired
    private ForumCommentMapper forumCommentMapper;

    // 1. 获取所有帖子
    @GetMapping("/posts")
    public Result getPosts() {
        List<ForumPost> list = forumMapper.getList();
        return Result.success(list);
    }

    /** 帖子详情；可选 userId 用于返回当前用户是否已赞/已藏 */
    @GetMapping("/posts/{id}")
    public Result getPost(@PathVariable("id") Long id, @RequestParam(value = "userId", required = false) Long userId) {
        ForumPost post = forumMapper.getById(id);
        if (post == null) {
            return Result.error("帖子不存在或已删除");
        }
        if (userId != null) {
            post.setVotedByMe(forumMapper.countAction(userId, id, "vote") > 0);
            post.setStarredByMe(forumMapper.countAction(userId, id, "star") > 0);
        }
        return Result.success(post);
    }

    /** 当前用户发布的帖子 */
    @GetMapping("/user/{userId}/posts")
    public Result listMyPosts(@PathVariable("userId") Long userId) {
        return Result.success(forumMapper.listPostsByAuthor(userId));
    }

    /** 当前用户点过赞的帖子 */
    @GetMapping("/user/{userId}/voted")
    public Result listVotedPosts(@PathVariable("userId") Long userId) {
        return Result.success(forumMapper.listPostsByUserAction(userId, "vote"));
    }

    /** 当前用户收藏的帖子 */
    @GetMapping("/user/{userId}/starred")
    public Result listStarredPosts(@PathVariable("userId") Long userId) {
        return Result.success(forumMapper.listPostsByUserAction(userId, "star"));
    }

    /** 当前用户浏览过的帖子（按最近浏览时间倒序，数据来自 sys_forum_post_view） */
    @GetMapping("/user/{userId}/viewed")
    public Result listViewedPosts(@PathVariable("userId") Long userId) {
        return Result.success(forumMapper.listPostsViewedByUser(userId));
    }

    /** 记录用户浏览帖子（写入/更新 sys_forum_post_view） */
    @PostMapping("/posts/{id}/view")
    public Result recordPostView(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> body) {
        if (body == null || body.get("userId") == null) {
            return Result.error("userId 不能为空");
        }
        Object userIdObj = body.get("userId");
        Long userId = (userIdObj instanceof Number)
                ? ((Number) userIdObj).longValue()
                : Long.valueOf(String.valueOf(userIdObj));
        if (forumMapper.getById(id) == null) {
            return Result.error("帖子不存在或已删除");
        }
        forumMapper.upsertPostView(userId, id);
        return Result.success("ok");
    }

    /** 帖子评论列表（按时间正序） */
    @GetMapping("/posts/{postId}/comments")
    public Result listComments(@PathVariable("postId") Long postId) {
        if (forumMapper.getById(postId) == null) {
            return Result.error("帖子不存在或已删除");
        }
        return Result.success(forumCommentMapper.listByPostId(postId));
    }

    /** 发表评论 */
    @PostMapping("/posts/{postId}/comments")
    public Result addComment(@PathVariable("postId") Long postId, @RequestBody java.util.Map<String, Object> body) {
        if (forumMapper.getById(postId) == null) {
            return Result.error("帖子不存在或已删除");
        }
        if (body == null || body.get("userId") == null) {
            return Result.error("userId 不能为空");
        }
        Object userIdObj = body.get("userId");
        Long userId = (userIdObj instanceof Number)
                ? ((Number) userIdObj).longValue()
                : Long.valueOf(String.valueOf(userIdObj));
        Object contentObj = body.get("content");
        if (contentObj == null) {
            return Result.error("评论内容不能为空");
        }
        String content = String.valueOf(contentObj).trim();
        if (content.isEmpty()) {
            return Result.error("评论内容不能为空");
        }
        if (content.length() > 2000) {
            return Result.error("评论内容不能超过 2000 字");
        }
        ForumComment comment = new ForumComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(content);
        forumCommentMapper.insert(comment);
        forumMapper.adjustComments(postId, 1);
        ForumComment saved = forumCommentMapper.selectById(comment.getId());
        try {
            com.example.admin.config.ForumHandler.broadcast("{\"type\":\"post_comment\", \"postId\":" + postId + "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success(saved);
    }

    /** 删除自己的评论 */
    @DeleteMapping("/comments/{id}")
    public Result deleteComment(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        ForumComment c = forumCommentMapper.selectById(id);
        if (c == null) {
            return Result.error("评论不存在");
        }
        if (c.getUserId() == null || !c.getUserId().equals(userId)) {
            return Result.error("无权删除该评论");
        }
        Long postId = c.getPostId();
        forumCommentMapper.deleteById(id);
        forumMapper.adjustComments(postId, -1);
        try {
            com.example.admin.config.ForumHandler.broadcast("{\"type\":\"post_comment\", \"postId\":" + postId + "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success("已删除");
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

    /** 作者删除自己的帖子（级联清理点赞/收藏与浏览记录） */
    @DeleteMapping("/posts/{id}")
    public Result deletePost(@PathVariable("id") Long id, @RequestParam("userId") Long userId) {
        ForumPost post = forumMapper.getById(id);
        if (post == null) {
            return Result.error("帖子不存在或已删除");
        }
        if (post.getUserId() == null || !post.getUserId().equals(userId)) {
            return Result.error("无权删除该帖子");
        }
        forumMapper.deleteAllActionsByPostId(id);
        forumMapper.deleteAllViewsByPostId(id);
        forumCommentMapper.deleteAllByPostId(id);
        int n = forumMapper.deletePostByAuthor(id, userId);
        if (n == 0) {
            return Result.error("删除失败");
        }
        try {
            com.example.admin.config.ForumHandler.broadcast("{\"type\":\"post_deleted\", \"postId\":" + id + "}");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Result.success("已删除");
    }
}
