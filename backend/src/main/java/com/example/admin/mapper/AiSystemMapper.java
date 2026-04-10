package com.example.admin.mapper;

import com.example.admin.entity.AiChat;
import com.example.admin.entity.AiQuizRecord;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AiSystemMapper {

   
    @Select("SELECT CAST(answers_detail AS CHAR CHARACTER SET utf8mb4) FROM sys_quiz_score WHERE user_id = #{userId} AND module_id = #{moduleId}")
    String getQuizDetailByModule(@Param("userId") Long userId, @Param("moduleId") String moduleId);

    // --- 2. 聊天室 ---
    @Select("SELECT role, content FROM sys_ai_chat WHERE user_id = #{userId} ORDER BY create_time ASC")
    List<AiChat> selectChatHistory(Long userId);

    @Insert("INSERT INTO sys_ai_chat (user_id, role, content) VALUES (#{userId}, #{role}, #{content})")
    int insertChat(AiChat chat);

    // --- 3. 强化练习 
    @Select("SELECT id, module_id, title, weakness_analysis, CAST(quiz_json AS CHAR CHARACTER SET utf8mb4) AS quizJson, " +
            "CAST(user_answers AS CHAR CHARACTER SET utf8mb4) AS userAnswers, score, status, create_time " +
            "FROM sys_ai_quiz_record " +
            "WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<AiQuizRecord> selectQuizList(Long userId);

 
    @Select("SELECT * FROM sys_ai_quiz_record WHERE id = #{id} AND user_id = #{userId}")
    AiQuizRecord selectQuizById(@Param("id") Long id, @Param("userId") Long userId);

    @Insert("INSERT INTO sys_ai_quiz_record (user_id, module_id, title, weakness_analysis, quiz_json, status, create_time) " +
            "VALUES (#{userId}, #{moduleId}, #{title}, #{weaknessAnalysis}, #{quizJson}, 0, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertAiQuiz(AiQuizRecord record);

    @Insert("INSERT INTO sys_ai_quiz_record (user_id, module_id, title, weakness_analysis, quiz_json, status, create_time) " +
            "VALUES (#{userId}, #{moduleId}, #{title}, #{weaknessAnalysis}, #{quizJson}, 9, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertGeneratingQuiz(AiQuizRecord record);

    @Update("UPDATE sys_ai_quiz_record SET title = #{title}, quiz_json = #{quizJson}, status = 0 WHERE id = #{id} AND user_id = #{userId}")
    int finishQuizGeneration(@Param("id") Long id, @Param("userId") Long userId, @Param("title") String title, @Param("quizJson") String quizJson);

    @Update("UPDATE sys_ai_quiz_record SET weakness_analysis = #{errorMsg}, status = -1 WHERE id = #{id} AND user_id = #{userId}")
    int markQuizGenerateFailed(@Param("id") Long id, @Param("userId") Long userId, @Param("errorMsg") String errorMsg);

    @Update("UPDATE sys_ai_quiz_record SET title = #{title} WHERE id = #{id} AND user_id = #{userId}")
    int renameQuiz(@Param("id") Long id, @Param("userId") Long userId, @Param("title") String title);

    @Update("UPDATE sys_ai_quiz_record SET score = #{score}, user_answers = #{userAnswers}, status = 1 " +
            "WHERE id = #{id} AND user_id = #{userId}")
    int updateQuizResult(@Param("id") Long id, @Param("score") Integer score, @Param("userAnswers") String userAnswers, @Param("userId") Long userId);

  
    @Delete("DELETE FROM sys_ai_quiz_record WHERE id = #{id} AND user_id = #{userId}")
    int deleteQuizById(@Param("id") Long id, @Param("userId") Long userId);
}