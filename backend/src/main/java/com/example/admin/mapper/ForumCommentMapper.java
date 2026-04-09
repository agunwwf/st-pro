package com.example.admin.mapper;

import com.example.admin.entity.ForumComment;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ForumCommentMapper {

    @Insert("INSERT INTO sys_forum_comment(post_id, user_id, content, create_time) VALUES(#{postId}, #{userId}, #{content}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ForumComment comment);

    @Select("SELECT c.id, c.post_id as postId, c.user_id as userId, c.content, c.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM sys_forum_comment c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "WHERE c.post_id = #{postId} " +
            "ORDER BY c.create_time ASC")
    List<ForumComment> listByPostId(@Param("postId") Long postId);

    @Select("SELECT c.id, c.post_id as postId, c.user_id as userId, c.content, c.create_time as createTime, " +
            "u.nickname as authorName, u.avatar as authorAvatar " +
            "FROM sys_forum_comment c " +
            "LEFT JOIN sys_user u ON c.user_id = u.id " +
            "WHERE c.id = #{id}")
    ForumComment selectById(@Param("id") Long id);

    @Delete("DELETE FROM sys_forum_comment WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    @Delete("DELETE FROM sys_forum_comment WHERE post_id = #{postId}")
    int deleteAllByPostId(@Param("postId") Long postId);
}
