package com.example.admin.mapper;

import com.example.admin.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User getByUsername(String username);

    @Update("UPDATE sys_user SET nickname=#{nickname}, avatar=#{avatar}, signature=#{signature} WHERE id=#{id}")
    int updateById(User user);

    @Insert("INSERT INTO sys_user(username, password, nickname, create_time) VALUES(#{username}, #{password}, #{nickname}, NOW())")
    int insert(User user);

    @Select("""
            SELECT id, username, nickname, avatar, signature
            FROM sys_user
            WHERE username LIKE CONCAT('%',#{keyword},'%')
               OR nickname LIKE CONCAT('%',#{keyword},'%')
            ORDER BY id DESC
            LIMIT 20
            """)
    List<User> search(@Param("keyword") String keyword);
}