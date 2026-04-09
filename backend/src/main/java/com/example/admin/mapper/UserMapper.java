package com.example.admin.mapper;

import com.example.admin.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {
    @Select("SELECT id, username, password, " +
            "COALESCE(nickname, username) AS nickname, " +
            "COALESCE(avatar, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png') AS avatar, " +
            "signature, role, teacher_id AS teacherId, gender, birthday, region, " +
            "COALESCE(progress, 0) AS progress, " +
            "COALESCE(check_in_count, 0) AS checkInCount, " +
            "is_model as isModel, create_time as createTime " +
            "FROM sys_user WHERE id = #{id}")
    User getById(Long id);

    @Select("SELECT id, username, password, " +
            "COALESCE(nickname, username) AS nickname, " +
            "COALESCE(avatar, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png') AS avatar, " +
            "signature, role, teacher_id AS teacherId, gender, birthday, region, " +
            "COALESCE(progress, 0) AS progress, " +
            "COALESCE(check_in_count, 0) AS checkInCount, " +
            "is_model as isModel, create_time as createTime " +
            "FROM sys_user WHERE username = #{username}")
    User getByUsername(String username);

//只有这个管理员拉取学生列表的方法，改用实时进度计算
    @Select("SELECT id, username, password, " +
            "COALESCE(nickname, username) AS nickname, " +
            "COALESCE(avatar, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png') AS avatar, " +
            "signature, role, teacher_id AS teacherId, gender, birthday, region, " +
           
            "IFNULL(ROUND((SELECT COUNT(*) FROM sys_learning_completion WHERE user_id = sys_user.id) / 10.0 * 100), 0) AS progress, " +
          
            "COALESCE(check_in_count, 0) AS checkInCount, " +
            "is_model as isModel, create_time as createTime " +
            "FROM sys_user WHERE role = 'STUDENT'")
    List<User> getAllStudents();

    @Select("SELECT id, username, password, " +
            "COALESCE(nickname, username) AS nickname, " +
            "COALESCE(avatar, 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png') AS avatar, " +
            "signature, role, teacher_id AS teacherId, gender, birthday, region, " +
            "COALESCE(progress, 0) AS progress, " +
            "COALESCE(check_in_count, 0) AS checkInCount, " +
            "is_model as isModel, create_time as createTime " +
            "FROM sys_user WHERE (username LIKE CONCAT('%',#{keyword},'%') OR nickname LIKE CONCAT('%',#{keyword},'%'))")
    List<User> searchUsers(String keyword);

    @Select("SELECT teacher_id FROM sys_user WHERE id = #{id}")
    Long getTeacherIdByUserId(@Param("id") Long id);

    @Update("UPDATE sys_user SET teacher_id = #{teacherId} WHERE id = #{id} AND role = 'STUDENT'")
    int setTeacherId(@Param("id") Long id, @Param("teacherId") Long teacherId);

    @Update("UPDATE sys_user SET teacher_id = NULL WHERE id = #{id} AND role = 'STUDENT'")
    int clearTeacherId(@Param("id") Long id);

    @Update("UPDATE sys_user SET nickname=#{nickname}, avatar=#{avatar}, signature=#{signature}, gender=#{gender}, birthday=#{birthday}, region=#{region}, progress=#{progress}, check_in_count=#{checkInCount}, is_model=#{isModel} WHERE id=#{id}")
    int updateById(User user);

    @Insert("INSERT INTO sys_user(username, password, nickname, avatar, signature, role, create_time) " +
            "VALUES(#{username}, #{password}, #{nickname}, #{avatar}, #{signature}, #{role}, NOW())")
    @org.apache.ibatis.annotations.Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Delete("DELETE FROM sys_user WHERE id = #{id} AND role = 'STUDENT'")
    int deleteStudentById(Long id);

    @Update("UPDATE sys_user SET is_model = #{isModel} WHERE id = #{id}")
    int updateModel(@Param("id") Long id, @Param("isModel") Integer isModel);

    // 打卡次数 +1（空则按 0 处理）
    @Update("UPDATE sys_user SET check_in_count = #{count} WHERE id = #{id}")
    int updateCheckInCount(@Param("id") Long id, @Param("count") Integer count);
}