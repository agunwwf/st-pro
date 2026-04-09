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
    @Update("UPDATE sys_friend SET status = 1 WHERE (user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})")
    int acceptRequest(@Param("userId") Long userId, @Param("friendId") Long friendId);

    // 建立好友关系（存在则更新为已同意，避免重复插入报错）
    @Insert("INSERT INTO sys_friend(user_id, friend_id, status, create_time) " +
            "VALUES(#{userId}, #{friendId}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE status = 1")
    int addFriendDirect(Friend friend);

    // 查询我的好友列表 (status=1)，兼容任意方向的历史数据，只保留每个好友一条记录
    @Select("SELECT " +
            "  MIN(f.id) AS id, " +
            "  #{userId} AS userId, " +
            "  CASE WHEN f.user_id = #{userId} THEN f.friend_id ELSE f.user_id END AS friendId, " +
            "  1 AS status, " +
            "  MIN(f.create_time) AS createTime, " +
            "  u.username AS friendUsername, " +
            "  u.nickname AS friendNickname, " +
            "  u.avatar AS friendAvatar, " +
            "  u.signature AS friendSignature, " +
            "  u.is_model AS friendIsModel " +
            "FROM sys_friend f " +
            "JOIN sys_user u ON u.id = CASE WHEN f.user_id = #{userId} THEN f.friend_id ELSE f.user_id END " +
            "WHERE (f.user_id = #{userId} OR f.friend_id = #{userId}) AND f.status = 1 " +
            "GROUP BY friendId, u.username, u.nickname, u.avatar, u.signature, u.is_model")
    List<Friend> getMyFriends(Long userId);

    // 查询收到的好友申请 (status=0)
    @Select("SELECT f.id, f.user_id as userId, f.friend_id as friendId, f.status, f.create_time as createTime, " +
            "u.username as friendUsername, u.nickname as friendNickname, u.avatar as friendAvatar, u.signature as friendSignature, u.is_model as friendIsModel " +
            "FROM sys_friend f " +
            "JOIN sys_user u ON f.user_id = u.id " +
            "WHERE f.friend_id = #{userId} AND f.status = 0")
    List<Friend> getFriendRequests(Long userId);

    // 检查是否已经是好友或已申请
    @Select("SELECT count(*) FROM sys_friend WHERE user_id = #{userId} AND friend_id = #{friendId}")
    int checkRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);

    // 删除好友（双向）
    @Delete("DELETE FROM sys_friend WHERE (user_id = #{userId} AND friend_id = #{friendId}) OR (user_id = #{friendId} AND friend_id = #{userId})")
    int deleteRelation(@Param("userId") Long userId, @Param("friendId") Long friendId);

    // 根据申请记录 ID 查询
    @Select("SELECT id, user_id as userId, friend_id as friendId, status, create_time as createTime " +
            "FROM sys_friend WHERE id = #{id}")
    Friend getById(Long id);
}