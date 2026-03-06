package com.example.admin.mapper;

import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CheckInMapper {
    @Insert("INSERT IGNORE INTO sys_check_in(user_id, check_date) VALUES(#{userId}, #{date})")
    int insert(@Param("userId") Long userId, @Param("date") String date);

    @Select("SELECT DATE_FORMAT(check_date, '%Y-%m-%d') FROM sys_check_in WHERE user_id = #{userId} ORDER BY check_date DESC")
    List<String> getCheckInDates(Long userId);
}