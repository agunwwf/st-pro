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

    // 3. 点赞/收藏操作 (简单实现：直接更新计数)
    @Update("UPDATE sys_forum_post SET votes = votes + #{count} WHERE id = #{id}")
    int updateVotes(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE sys_forum_post SET stars = stars + #{count} WHERE id = #{id}")
    int updateStars(@Param("id") Long id, @Param("count") int count);

    // 4. 点赞/收藏：每个用户同一个帖子只允许一次（基于 sys_forum_action）
    @Select("SELECT count(*) FROM sys_forum_action WHERE user_id = #{userId} AND post_id = #{postId} AND action_type = #{actionType}")
    int countAction(@Param("userId") Long userId, @Param("postId") Long postId, @Param("actionType") String actionType);

    @Insert("INSERT INTO sys_forum_action(user_id, post_id, action_type) VALUES(#{userId}, #{postId}, #{actionType})")
    int insertAction(@Param("userId") Long userId, @Param("postId") Long postId, @Param("actionType") String actionType);

    @Delete("DELETE FROM sys_forum_action WHERE user_id = #{userId} AND post_id = #{postId} AND action_type = #{actionType}")
    int deleteAction(@Param("userId") Long userId, @Param("postId") Long postId, @Param("actionType") String actionType);
}
