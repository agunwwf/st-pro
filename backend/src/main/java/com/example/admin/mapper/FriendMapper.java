package com.example.admin.mapper;

import com.example.admin.entity.Friend;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FriendMapper {

    // 发送好友申请
    @Insert("INSERT INTO sys_friend(user_id, friend_id, status, create_time) VALUES(#{userId}, #{friendId}, 0, NOW())")
    int insertRequest(Friend friend);

    // 同意好友申请
    @Update("UPDATE sys_friend SET status = 1 WHERE user_id = #{friendId} AND friend_id = #{userId}")
    int acceptRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    // 建立双向关系
    @Insert("INSERT INTO sys_friend(user_id, friend_id, status, create_time) VALUES(#{userId}, #{friendId}, 1, NOW())")
    int addFriendDirect(Friend friend);

    // 查询我的好友列表 (status=1)
    // 使用别名确保映射正确：f.user_id as userId
    @Select("SELECT f.id, f.user_id as userId, f.friend_id as friendId, f.status, f.create_time as createTime, " +
            "u.username as friendUsername, u.nickname as friendNickname, u.avatar as friendAvatar " +
            "FROM sys_friend f " +
            "LEFT JOIN sys_user u ON f.friend_id = u.id " +
            "WHERE f.user_id = #{userId} AND f.status = 1")
    List<Friend> getMyFriends(Long userId);

    // 查询收到的好友申请 (status=0)
    @Select("SELECT f.id, f.user_id as userId, f.friend_id as friendId, f.status, f.create_time as createTime, " +
            "u.username as friendUsername, u.nickname as friendNickname, u.avatar as friendAvatar " +
            "FROM sys_friend f " +
            "LEFT JOIN sys_user u ON f.user_id = u.id " +
            "WHERE f.friend_id = #{userId} AND f.status = 0")
    List<Friend> getFriendRequests(Long userId);

    // 检查是否已经是好友或已申请
    @Select("SELECT count(*) FROM sys_friend WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int checkRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);
}