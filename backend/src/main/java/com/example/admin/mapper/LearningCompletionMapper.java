package com.example.admin.mapper;

import com.example.admin.entity.LearningCompletion;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LearningCompletionMapper {

    /**
     * 与 LearningThreadMapper.insert 一样：用实体传参，SQL 里 #{userId} 对应 getUserId()。
     * 插入时只需设置 userId、moduleId、kind；id 由表自增，create_time 用默认值。
     */
    @Insert("INSERT INTO sys_learning_completion (user_id, module_id, kind) VALUES (#{userId}, #{moduleId}, #{kind}) "
            + "ON DUPLICATE KEY UPDATE id = id")
    int upsert(LearningCompletion row);

    @Select("SELECT id, user_id AS userId, module_id AS moduleId, kind, create_time AS createTime "
            + "FROM sys_learning_completion WHERE user_id = #{userId} ORDER BY create_time ASC")
    List<LearningCompletion> listByUserId(Long userId);

    @Select("SELECT COUNT(*) FROM sys_learning_completion WHERE user_id = #{userId}")
    int countByUserId(Long userId);
}
