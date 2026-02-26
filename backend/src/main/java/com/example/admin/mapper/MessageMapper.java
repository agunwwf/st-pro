package com.example.admin.mapper;

import com.example.admin.entity.Message;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface MessageMapper {

    @Insert("INSERT INTO sys_message(from_user, to_user, content, msg_type, create_time) " +
            "VALUES(#{fromUser}, #{toUser}, #{content}, #{msgType}, #{createTime})")
    int insert(Message message);

    // 获取两人之间的聊天记录
    @Select("SELECT id, from_user as fromUser, to_user as toUser, content, msg_type as msgType, create_time as createTime " +
            "FROM sys_message " +
            "WHERE (from_user = #{user1} AND to_user = #{user2}) " +
            "   OR (from_user = #{user2} AND to_user = #{user1}) " +
            "ORDER BY create_time ASC")
    List<Message> getHistory(String user1, String user2);
}