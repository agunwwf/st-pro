package com.example.admin.mapper;

import com.example.admin.entity.ForumPost;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ForumMapper {

    // 1. 发帖
    @Insert("INSERT INTO sys_forum_post(user_id, title, content, section, cover, create_time) " +
            "VALUES(#{userId}, #{title}, #{content}, #{section}, #{cover}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ForumPost post);

    // 2. 获取列表 (关联用户表查询作者名和头像)
    @Select("SELECT p.id, p.user_id as userId, p.title, p.content, p.section, p.cover, " +
            "p.votes, p.stars, p.comments, p.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM sys_forum_post p " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "ORDER BY p.create_time DESC")
    List<ForumPost> getList();

    @Select("SELECT p.id, p.user_id as userId, p.title, p.content, p.section, p.cover, " +
            "p.votes, p.stars, p.comments, p.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM sys_forum_post p " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "WHERE p.id = #{id}")
    ForumPost getById(@Param("id") Long id);

    @Select("SELECT p.id, p.user_id as userId, p.title, p.content, p.section, p.cover, " +
            "p.votes, p.stars, p.comments, p.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM sys_forum_post p " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "WHERE p.user_id = #{userId} " +
            "ORDER BY p.create_time DESC")
    List<ForumPost> listPostsByAuthor(@Param("userId") Long userId);

    @Select("SELECT p.id, p.user_id as userId, p.title, p.content, p.section, p.cover, " +
            "p.votes, p.stars, p.comments, p.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM sys_forum_post p " +
            "INNER JOIN sys_forum_action a ON a.post_id = p.id AND a.user_id = #{userId} AND a.action_type = #{actionType} " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "ORDER BY a.create_time DESC")
    List<ForumPost> listPostsByUserAction(@Param("userId") Long userId, @Param("actionType") String actionType);

    @Insert("INSERT INTO sys_forum_post_view(user_id, post_id, last_view_time) VALUES(#{userId}, #{postId}, NOW()) " +
            "ON DUPLICATE KEY UPDATE last_view_time = NOW()")
    int upsertPostView(@Param("userId") Long userId, @Param("postId") Long postId);

    @Select("SELECT p.id, p.user_id as userId, p.title, p.content, p.section, p.cover, " +
            "p.votes, p.stars, p.comments, p.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar, " +
            "v.last_view_time as lastViewTime " +
            "FROM sys_forum_post p " +
            "INNER JOIN sys_forum_post_view v ON v.post_id = p.id AND v.user_id = #{userId} " +
            "LEFT JOIN sys_user u ON p.user_id = u.id " +
            "ORDER BY v.last_view_time DESC")
    List<ForumPost> listPostsViewedByUser(@Param("userId") Long userId);

    // 3. 点赞/收藏操作 (简单实现：直接更新计数)
    @Update("UPDATE sys_forum_post SET votes = votes + #{count} WHERE id = #{id}")
    int updateVotes(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE sys_forum_post SET stars = stars + #{count} WHERE id = #{id}")
    int updateStars(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE sys_forum_post SET comments = GREATEST(comments + #{delta}, 0) WHERE id = #{postId}")
    int adjustComments(@Param("postId") Long postId, @Param("delta") int delta);

    // 4. 点赞/收藏：每个用户同一个帖子只允许一次（基于 sys_forum_action）
    @Select("SELECT count(*) FROM sys_forum_action WHERE user_id = #{userId} AND post_id = #{postId} AND action_type = #{actionType}")
    int countAction(@Param("userId") Long userId, @Param("postId") Long postId, @Param("actionType") String actionType);

    @Insert("INSERT INTO sys_forum_action(user_id, post_id, action_type) VALUES(#{userId}, #{postId}, #{actionType})")
    int insertAction(@Param("userId") Long userId, @Param("postId") Long postId, @Param("actionType") String actionType);

    @Delete("DELETE FROM sys_forum_action WHERE user_id = #{userId} AND post_id = #{postId} AND action_type = #{actionType}")
    int deleteAction(@Param("userId") Long userId, @Param("postId") Long postId, @Param("actionType") String actionType);

    @Delete("DELETE FROM sys_forum_action WHERE post_id = #{postId}")
    int deleteAllActionsByPostId(@Param("postId") Long postId);

    @Delete("DELETE FROM sys_forum_post_view WHERE post_id = #{postId}")
    int deleteAllViewsByPostId(@Param("postId") Long postId);

    /** 仅当 id 与作者 user_id 匹配时删除帖子 */
    @Delete("DELETE FROM sys_forum_post WHERE id = #{id} AND user_id = #{userId}")
    int deletePostByAuthor(@Param("id") Long id, @Param("userId") Long userId);
}
