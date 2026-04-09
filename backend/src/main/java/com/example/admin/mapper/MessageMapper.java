package com.example.admin.mapper;

import com.example.admin.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO sys_message(from_user, to_user, content, msg_type, file_name, create_time) " +
            "VALUES(#{fromUser}, #{toUser}, #{content}, #{msgType}, #{fileName}, #{createTime})")
    int insert(Message message);

    @Select("SELECT id, from_user as fromUser, to_user as toUser, content, msg_type as msgType, file_name as fileName, create_time as createTime " +
            "FROM sys_message " +
            "WHERE (from_user = #{user1} AND to_user = #{user2}) " +
            "   OR (from_user = #{user2} AND to_user = #{user1}) " +
            "ORDER BY create_time ASC")
    List<Message> getHistory(String user1, String user2);

    @Select("SELECT id, from_user as fromUser, to_user as toUser, content, msg_type as msgType, file_name as fileName, create_time as createTime " +
            "FROM sys_message " +
            "WHERE ((from_user = #{user1} AND to_user = #{user2}) OR (from_user = #{user2} AND to_user = #{user1})) " +
            "  AND create_time > #{afterTime} " +
            "ORDER BY create_time ASC")
    List<Message> getHistoryAfter(@Param("user1") String user1, @Param("user2") String user2, @Param("afterTime") LocalDateTime afterTime);

    @Update("CREATE TABLE IF NOT EXISTS sys_message_clear_log (" +
            "id BIGINT PRIMARY KEY AUTO_INCREMENT," +
            "username VARCHAR(50) NOT NULL," +
            "peer_username VARCHAR(50) NOT NULL," +
            "clear_time DATETIME NOT NULL," +
            "UNIQUE KEY uk_user_peer (username, peer_username)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
    void ensureClearLogTable();

    @Insert("INSERT INTO sys_message_clear_log(username, peer_username, clear_time) " +
            "VALUES(#{username}, #{peerUsername}, #{clearTime}) " +
            "ON DUPLICATE KEY UPDATE clear_time = VALUES(clear_time)")
    int upsertClearTime(@Param("username") String username, @Param("peerUsername") String peerUsername, @Param("clearTime") LocalDateTime clearTime);

    @Select("SELECT clear_time FROM sys_message_clear_log WHERE username = #{username} AND peer_username = #{peerUsername} LIMIT 1")
    LocalDateTime getClearTime(@Param("username") String username, @Param("peerUsername") String peerUsername);
}