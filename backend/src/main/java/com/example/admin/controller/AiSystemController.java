package com.example.admin.controller;

import com.example.admin.entity.AiChat;
import com.example.admin.entity.AiQuizRecord;
import com.example.admin.mapper.AiSystemMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/ai")
public class AiSystemController {

    @Autowired
    private AiSystemMapper aiSystemMapper;

    /** 复用 Spring AI（application.yml 里的 DeepSeek base-url + api-key + model） */
    private final ChatClient chatClient;
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TUTOR_SYSTEM = """
            你是一个机器学习学习平台的【算法导师】。
            你会根据学生的测验答题病历（JSON）给出诊断：
            1) 肯定做对的知识点；
            2) 指出做错题反映的薄弱概念；
            3) 用通俗语言讲清关键点；
            4) 最后加一句：如果你觉得搞懂了，可以点击下方按钮进入【强化练习】！
            要求：专业但鼓励，条理清晰，不要跑题。
            """;

    public AiSystemController(ChatClient.Builder builder) {
        this.chatClient = builder.defaultSystem(TUTOR_SYSTEM).build();
    }

    @GetMapping("/analysis/source")
    public Result getDiagnosisSource(@RequestAttribute("userId") Long userId, @RequestParam String moduleId) {
        String detailJson = aiSystemMapper.getQuizDetailByModule(userId, moduleId);
        if (detailJson == null || detailJson.trim().isEmpty()) {
            return Result.error("您尚未完成该模块的测验，请先前往学习并提交。");
        }
        Result<String> r = new Result<>();
        r.setCode(200);
        r.setMsg("操作成功");
        r.setData(detailJson);
        return r;
    }

    /**
     * 直接在后端调用大模型
     * Body: { moduleId: "kmeans", moduleName: "K-Means 聚类" }
     */
    @PostMapping("/analysis/run")
    public Result<String> runAnalysis(@RequestAttribute("userId") Long userId, @RequestBody Map<String, String> body) {
        String moduleId = body.getOrDefault("moduleId", "").trim();
        String moduleName = body.getOrDefault("moduleName", moduleId).trim();
        if (moduleId.isEmpty()) {
            return Result.error("moduleId 不能为空");
        }
        String detailJson = aiSystemMapper.getQuizDetailByModule(userId, moduleId);
        if (detailJson == null || detailJson.trim().isEmpty()) {
            return Result.error("未找到该模块的测验记录");
        }
        String prompt = """
                学生刚完成了【%s】模块测验，答题详情如下（JSON）：
                %s
                """.formatted(moduleName, detailJson);
        try {
            String reply = chatClient.prompt().user(prompt).call().content();
            // 避免命中 Result.success(String msg) 重载：显式把回复放到 data
            Result<String> r = new Result<>();
            r.setCode(200);
            r.setMsg("操作成功");
            r.setData(reply);
            return r;
        } catch (Exception e) {
            return Result.error("导师诊断失败：" + e.getMessage());
        }
    }

    /**
     * 普通聊天也走后端调用大模型（替代前端 useDeepSeek.js）。
     * Body: { messages: [ {role:"user|assistant", content:"..."}, ... ] }
     */
    @PostMapping("/chat/complete")
    public Result<String> completeChat(@RequestBody Map<String, Object> body) {
        Object messagesObj = body.get("messages");
        if (!(messagesObj instanceof List<?> list) || list.isEmpty()) {
            return Result.error("messages 不能为空");
        }
        // 这里用最简单的方式：把最近几轮对话拼成文本，交给大模型回答
        StringBuilder sb = new StringBuilder();
        int start = Math.max(0, list.size() - 12);
        for (int i = start; i < list.size(); i++) {
            Object o = list.get(i);
            if (o instanceof Map<?, ?> m) {
                Object role = m.get("role");
                Object content = m.get("content");
                if (role != null && content != null) {
                    sb.append(role).append("：").append(content).append("\n");
                }
            }
        }
        try {
            String reply = chatClient.prompt().user(sb.toString()).call().content();
            // 避免命中 Result.success(String msg) 重载：显式把回复放到 data
            Result<String> r = new Result<>();
            r.setCode(200);
            r.setMsg("操作成功");
            r.setData(reply);
            return r;
        } catch (Exception e) {
            return Result.error("对话失败：" + e.getMessage());
        }
    }

    @GetMapping("/chat/history")
    public Result getChatHistory(@RequestAttribute("userId") Long userId) {
        return Result.success(aiSystemMapper.selectChatHistory(userId));
    }

    @PostMapping("/chat/save")
    public Result saveChat(@RequestBody AiChat chat, @RequestAttribute("userId") Long userId) {
        chat.setUserId(userId); // 覆写，无视前端假数据
        aiSystemMapper.insertChat(chat);
        return Result.success("聊天保存成功");
    }

    @GetMapping("/quiz/list")
    public Result getQuizList(@RequestAttribute("userId") Long userId) {
        return Result.success(aiSystemMapper.selectQuizList(userId));
    }

    @GetMapping("/quiz/{id}")
    public Result getQuizDetail(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        return Result.success(aiSystemMapper.selectQuizById(id, userId));
    }

    @PostMapping("/quiz/save")
    public Result saveQuiz(@RequestBody AiQuizRecord record, @RequestAttribute("userId") Long userId) {
        record.setUserId(userId); // 覆写
        aiSystemMapper.insertAiQuiz(record);
        return Result.success(record.getId());
    }

    /**
     * 异步生成试卷：请求返回后后端继续执行，前端离开页面不会中断。
     * status: 9=生成中, 0=未完成可作答, 1=已提交, -1=生成失败
     */
    @PostMapping("/quiz/generate")
    public Result<Map<String, Object>> generateQuiz(@RequestBody Map<String, String> body, @RequestAttribute("userId") Long userId) {
        String moduleId = body.getOrDefault("moduleId", "").trim();
        String moduleName = body.getOrDefault("moduleName", moduleId).trim();
        if (moduleId.isEmpty() || moduleName.isEmpty()) {
            return Result.error("moduleId/moduleName 不能为空");
        }

        AiQuizRecord record = new AiQuizRecord();
        record.setUserId(userId);
        record.setModuleId(moduleId);
        record.setTitle(moduleName + " 试卷生成中...");
        record.setWeaknessAnalysis("生成中");
        record.setQuizJson("{}");
        aiSystemMapper.insertGeneratingQuiz(record);

        Long recordId = record.getId();
        CompletableFuture.runAsync(() -> {
            try {
                String prompt = """
                        你是一个严谨的计算机科学出卷专家。请根据【%s】模块的重点，生成一套专属强化试卷。
                        【试卷结构】：10道单选题，5道填空题，1道算法编程题（Python或C++）。
                        【严禁捏造】：所有题目必须从权威教材、知名竞赛、或大厂面试题中提取或改编。
                        【极度重要-输出格式】：你必须且只能输出一个合法的 JSON 对象，绝对不要包含任何 Markdown 标记（如 ```json ），不要输出任何其他文字。JSON 模板如下：
                        {
                          "title": "%s 算法原理与权威真题强化卷",
                          "mcq": [
                            { "id": "m1", "question": "题目...", "options": ["A. x", "B. y", "C. z", "D. w"], "answer": "A", "explanation": "解析...", "source": "出自: XXX" }
                          ],
                          "fill_in_the_blank": [
                            { "id": "f1", "question": "题目...", "answer": "答案...", "explanation": "解析...", "source": "出自: XXX" }
                          ],
                          "coding": {
                            "id": "c1", "question": "题目...", "language": "python", "template": "def func():\\n    pass", "answer": "标准代码...", "explanation": "解析...", "source": "出自: XXX"
                          }
                        }
                        """.formatted(moduleName, moduleName);

                String raw = chatClient.prompt().user(prompt).call().content();
                String clean = raw.replace("```json", "").replace("```", "").trim();
                JsonNode root = OBJECT_MAPPER.readTree(clean);
                String normalizedJson = OBJECT_MAPPER.writeValueAsString(root);
                String title = root.path("title").asText(moduleName + " 专项强化练习");
                aiSystemMapper.finishQuizGeneration(recordId, userId, title, normalizedJson);
            } catch (Exception e) {
                aiSystemMapper.markQuizGenerateFailed(recordId, userId, "生成失败：" + e.getMessage());
            }
        });

        Map<String, Object> data = new HashMap<>();
        data.put("id", recordId);
        data.put("status", 9);
        return Result.success(data);
    }

    @PostMapping("/quiz/submit")
    public Result submitQuiz(@RequestBody AiQuizRecord record, @RequestAttribute("userId") Long userId) {
        aiSystemMapper.updateQuizResult(record.getId(), record.getScore(), record.getUserAnswers(), userId);
        return Result.success("成绩已存档");
    }

    @DeleteMapping("/quiz/{id}")
    public Result deleteQuiz(@PathVariable Long id, @RequestAttribute("userId") Long userId) {
        aiSystemMapper.deleteQuizById(id, userId);
        return Result.success("记录已删除");
    }

    @PostMapping("/quiz/{id}/rename")
    public Result renameQuiz(@PathVariable Long id, @RequestBody Map<String, String> body, @RequestAttribute("userId") Long userId) {
        String title = body.getOrDefault("title", "").trim();
        if (title.isEmpty()) return Result.error("标题不能为空");
        aiSystemMapper.renameQuiz(id, userId, title);
        return Result.success("重命名成功");
    }
}