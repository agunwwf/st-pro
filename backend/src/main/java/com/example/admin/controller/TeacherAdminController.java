package com.example.admin.controller;

import com.example.admin.entity.User;
import com.example.admin.mapper.StudentExamMapper;
import com.example.admin.mapper.TeacherAdminMapper;
import com.example.admin.mapper.UserMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/teacher")
public class TeacherAdminController {

    @Autowired
    private TeacherAdminMapper teacherAdminMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private StudentExamMapper studentExamMapper;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String TEACHER_PAPER_SYSTEM = """
            你是一个严谨的【高校老师出卷助教】。
            你的目标：根据一场班级考试的统计薄弱点，为老师生成一张“针对性训练卷”。
            规则：
            1) 题目必须围绕薄弱点（低正确率/常错点）；
            2) 选择题必须给出 4 个选项，且标准答案必须是 A/B/C/D 中的一个；
            3) 填空题标准答案给出核心关键词/关键数值即可；
            4) 编程题给出：题干 + 标准答案（可写参考实现或关键思路；若不便给全代码，可留空，但要给出可判分点）；
            5) 必须输出严格合法 JSON，禁止 Markdown 代码块，禁止多余文字。
            输出 JSON 格式固定为：
            {
              "title": "试卷标题",
              "questions": [
                { "type": "选择题", "content": "题干", "options": ["...","...","...","..."], "standardAnswer": "A" },
                { "type": "填空题", "content": "题干", "standardAnswer": "..." },
                { "type": "编程题", "content": "题干", "standardAnswer": "..." }
              ]
            }
            """;

    private final ChatClient teacherPaperClient;

    public TeacherAdminController(ChatClient.Builder builder) {
        this.teacherPaperClient = builder.defaultSystem(TEACHER_PAPER_SYSTEM).build();
    }

    private Long getUserIdFromToken(HttpServletRequest request) {
        Object userIdObj = request.getAttribute("userId");
        if (userIdObj != null) {
            return Long.valueOf(userIdObj.toString());
        }
        return null;
    }

    private boolean isAdmin(HttpServletRequest request) {
        Long uid = getUserIdFromToken(request);
        if (uid == null) return false;
        User u = userMapper.getById(uid);
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    @GetMapping("/list-teachers")
    public Result<List<Map<String, Object>>> listTeachers() {
        return Result.success(teacherAdminMapper.listAllTeachers());
    }

    @GetMapping("/my-students")
    public Result<List<Map<String, Object>>> myStudents(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        return Result.success(teacherAdminMapper.listMyStudents(teacherId));
    }

    @GetMapping("/lms/questions")
    public Result<List<Map<String, Object>>> getQuestions(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");

        return Result.success(teacherAdminMapper.listQuestionsPaged(0, 50, null, null, null));
    }

    @GetMapping("/lms/questions/page")
    public Result<Map<String, Object>> getQuestionsPaged(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword
    ) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        if (page < 1) page = 1;
        if (size < 1) size = 1;
        if (size > 200) size = 200; // 防被恶意拉爆

        int offset = (page - 1) * size;
        long total = teacherAdminMapper.countQuestions(category, type, keyword);
        List<Map<String, Object>> items = teacherAdminMapper.listQuestionsPaged(offset, size, category, type, keyword);

        Map<String, Object> data = new java.util.HashMap<>();
        data.put("page", page);
        data.put("size", size);
        data.put("total", total);
        data.put("items", items);
        return Result.success(data);
    }

    @GetMapping("/lms/papers")
    public Result<List<Map<String, Object>>> getPapers(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        return Result.success(teacherAdminMapper.listMyPapers(teacherId));
    }

    @PostMapping("/lms/paper/create")
    @Transactional
    public Result<Object> createPaper(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        req.put("teacherId", teacherId);
        teacherAdminMapper.insertPaper(req);

        Long paperId = Long.valueOf(req.get("id").toString());
        @SuppressWarnings("unchecked")
        List<Object> questionIds = (List<Object>) req.get("questionIds");
        if (questionIds == null || questionIds.isEmpty()) {
            return Result.error("请至少选择一道题");
        }
        for (Object qIdObj : questionIds) {
            long qid = qIdObj instanceof Number ? ((Number) qIdObj).longValue() : Long.parseLong(qIdObj.toString().trim());
            teacherAdminMapper.insertPaperQuestion(paperId, qid);
        }
        return Result.success((Object) null);
    }

    /** 教师自拟题：写入题库并可在组卷时选用 */
    @PostMapping("/lms/question/manual")
    @Transactional
    public Result<Map<String, Object>> createManualQuestion(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        String type = req.get("type") == null ? "" : req.get("type").toString().trim();
        String content = req.get("content") == null ? "" : req.get("content").toString().trim();
        String standardAnswer = req.get("standardAnswer") == null ? "" : req.get("standardAnswer").toString().trim();
        String category = req.get("category") == null ? "" : req.get("category").toString().trim();

        if (content.isEmpty()) {
            return Result.error("题干不能为空");
        }
        if (!type.equals("选择题") && !type.equals("填空题") && !type.equals("编程题")) {
            return Result.error("题型须为：选择题、填空题、编程题");
        }
        if (standardAnswer.isEmpty() && (type.equals("选择题") || type.equals("填空题"))) {
            return Result.error("选择题与填空题请填写标准答案（用于自动判分）");
        }

        List<String> opts = new ArrayList<>();
        if (type.equals("选择题")) {
            Object optObj = req.get("options");
            if (optObj instanceof List) {
                for (Object x : (List<?>) optObj) {
                    if (x != null && !x.toString().trim().isEmpty()) {
                        opts.add(x.toString().trim());
                    }
                }
            }
            if (opts.size() < 2) {
                return Result.error("选择题至少需要 2 个非空选项");
            }
        }

        String optionsJson;
        try {
            optionsJson = OBJECT_MAPPER.writeValueAsString(opts);
        } catch (Exception e) {
            return Result.error("选项数据无效");
        }

        String stdForDb = standardAnswer.isEmpty() ? " " : standardAnswer;

        Map<String, Object> row = new HashMap<>();
        row.put("teacherId", teacherId);
        row.put("category", category.isEmpty() ? "kmeans" : category);
        row.put("type", type);
        row.put("content", content);
        row.put("optionsJson", optionsJson);
        row.put("standardAnswer", stdForDb);
        teacherAdminMapper.insertTeacherQuestion(row);

        Map<String, Object> out = new HashMap<>();
        out.put("id", row.get("id"));
        out.put("type", type);
        out.put("content", content);
        out.put("options", optionsJson);
        out.put("standardAnswer", standardAnswer);
        return Result.success(out);
    }

    @PostMapping("/lms/assignment/publish")
    public Result<Object> publishAssignment(HttpServletRequest request, @RequestBody Map<String, Object> req) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        req.put("teacherId", teacherId);
        teacherAdminMapper.insertAssignment(req);
        return Result.success((Object) null);
    }

    @GetMapping("/lms/assignments")
    public Result<List<Map<String, Object>>> getAssignments(HttpServletRequest request) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        return Result.success(teacherAdminMapper.listMyAssignments(teacherId));
    }

    /** 查看试卷模板详情（含题目与标准答案），用于教师自查组卷 */
    @GetMapping("/lms/paper/{paperId}/detail")
    public Result<Map<String, Object>> paperDetail(HttpServletRequest request, @PathVariable Long paperId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        Map<String, Object> paper = teacherAdminMapper.getPaperForTeacher(paperId, teacherId);
        if (paper == null) return Result.error("试卷不存在");
        List<Map<String, Object>> questions = teacherAdminMapper.listPaperQuestions(paperId);
        Map<String, Object> data = new HashMap<>();
        data.put("paper", paper);
        data.put("questions", questions);
        return Result.success(data);
    }

    /** 修改试卷模板标题与题目内容（用于二次编辑） */
    @PostMapping("/lms/paper/{paperId}/update")
    @Transactional
    public Result<Object> updatePaper(
            HttpServletRequest request,
            @PathVariable Long paperId,
            @RequestBody Map<String, Object> body
    ) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        Map<String, Object> paper = teacherAdminMapper.getPaperForTeacher(paperId, teacherId);
        if (paper == null) return Result.error("试卷不存在");

        String title = body.get("title") == null ? "" : String.valueOf(body.get("title")).trim();
        if (title.isEmpty()) return Result.error("试卷标题不能为空");
        teacherAdminMapper.renamePaper(paperId, teacherId, title);

        Object questionsObj = body.get("questions");
        if (questionsObj instanceof List<?> list) {
            for (Object qObj : list) {
                if (!(qObj instanceof Map<?, ?> qMapRaw)) continue;
                Long qid;
                try {
                    Object idObj = qMapRaw.get("id");
                    qid = idObj == null ? null : Long.valueOf(String.valueOf(idObj));
                } catch (Exception e) {
                    qid = null;
                }
                if (qid == null) continue;

                String type = qMapRaw.get("type") == null ? "" : String.valueOf(qMapRaw.get("type")).trim();
                String content = qMapRaw.get("content") == null ? "" : String.valueOf(qMapRaw.get("content")).trim();
                String standardAnswer = qMapRaw.get("standardAnswer") == null ? "" : String.valueOf(qMapRaw.get("standardAnswer")).trim();
                if (content.isEmpty()) continue;
                if (!"选择题".equals(type) && !"填空题".equals(type) && !"编程题".equals(type)
                        && !"SINGLE_CHOICE".equalsIgnoreCase(type) && !"FILL_BLANK".equalsIgnoreCase(type) && !"CODING".equalsIgnoreCase(type)) {
                    continue;
                }

                List<String> opts = new ArrayList<>();
                if ("选择题".equals(type) || "SINGLE_CHOICE".equalsIgnoreCase(type)) {
                    Object optionsObj = qMapRaw.get("options");
                    if (optionsObj instanceof List<?> optsList) {
                        for (Object x : optsList) {
                            if (x != null && !x.toString().trim().isEmpty()) {
                                opts.add(x.toString().trim());
                            }
                        }
                    }
                    if (opts.size() < 2) continue;
                }
                String optionsJson;
                try {
                    optionsJson = OBJECT_MAPPER.writeValueAsString(opts);
                } catch (Exception e) {
                    continue;
                }
                teacherAdminMapper.updateQuestionEditableFields(
                        qid, type, content, optionsJson, standardAnswer.isEmpty() ? " " : standardAnswer
                );
            }
        }
        return Result.success((Object) null);
    }

    /** 删除试卷模板：若已被考试任务引用则禁止删除 */
    @DeleteMapping("/lms/paper/{paperId}")
    @Transactional
    public Result<Object> deletePaper(HttpServletRequest request, @PathVariable Long paperId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        if (teacherAdminMapper.getPaperForTeacher(paperId, teacherId) == null) {
            return Result.error("试卷不存在");
        }
        if (teacherAdminMapper.countAssignmentsForPaper(paperId) > 0) {
            return Result.error("该模板已被考试任务使用，请先删除对应考试任务");
        }
        teacherAdminMapper.deletePaperQuestions(paperId);
        teacherAdminMapper.deletePaper(paperId, teacherId);
        return Result.success((Object) null);
    }

    /** 删除已发布的考试任务（同时删除学生答卷记录） */
    @DeleteMapping("/lms/assignment/{assignmentId}")
    @Transactional
    public Result<Object> deleteAssignment(HttpServletRequest request, @PathVariable Long assignmentId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        if (teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId) == null) {
            return Result.error("考试任务不存在");
        }
        teacherAdminMapper.deleteStudentRecordsForAssignment(assignmentId);
        teacherAdminMapper.deleteAssignment(assignmentId, teacherId);
        return Result.success((Object) null);
    }

   
    @GetMapping("/lms/assignment/{assignmentId}/analytics")
    public Result<Map<String, Object>> assignmentAnalytics(HttpServletRequest request, @PathVariable Long assignmentId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        Map<String, Object> assignRow = teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId);
        if (assignRow == null) return Result.error("考试任务不存在");
        Map<String, Object> assign = new HashMap<>(assignRow);
        Map<String, Object> examTitleRow = studentExamMapper.getExamDetail(assignmentId);
        if (examTitleRow != null && examTitleRow.get("paperTitle") != null) {
            assign.put("paperTitle", examTitleRow.get("paperTitle"));
        }

        LocalDateTime end = toLocalDateTime(assign.get("endTime"));
        boolean examEnded = end == null || !LocalDateTime.now(ZoneId.systemDefault()).isBefore(end);

        int classSize = teacherAdminMapper.countStudentsInClass(teacherId);
        List<Map<String, Object>> rawRecords = teacherAdminMapper.listRecordsForAssignment(assignmentId);
        List<Map<String, Object>> submittedRows = new ArrayList<>();
        double scoreSum = 0.0;
        for (Map<String, Object> row : rawRecords) {
            int st = row.get("status") == null ? 0 : Integer.parseInt(row.get("status").toString());
            if (st >= 1) {
                submittedRows.add(row);
                if (row.get("score") != null) {
                    scoreSum += Double.parseDouble(row.get("score").toString());
                }
            }
        }
        int submittedCount = submittedRows.size();
        int notSubmittedCount = Math.max(0, classSize - submittedCount);
        BigDecimal avg = submittedCount > 0
                ? BigDecimal.valueOf(scoreSum / submittedCount).setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Map<String, Object>> questions = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);
        List<Map<String, Object>> questionStats = new ArrayList<>();
        int idx = 1;
        for (Map<String, Object> q : questions) {
            Long qid = Long.valueOf(q.get("id").toString());
            String type = q.get("type") == null ? "" : q.get("type").toString();
            String std = q.get("standardAnswer") == null ? null : q.get("standardAnswer").toString();
            int correct = 0;
            int blank = 0;
            int wrong = 0;
            for (Map<String, Object> rec : submittedRows) {
                String json = rec.get("answersJson") == null ? null : rec.get("answersJson").toString();
                JsonNode root;
                try {
                    root = OBJECT_MAPPER.readTree(json == null || json.isBlank() ? "{}" : json);
                } catch (Exception e) {
                    root = OBJECT_MAPPER.createObjectNode();
                }
                String ans = readAnswerForQuestion(root, qid);
                if (ans.isEmpty()) {
                    blank++;
                } else if (answersMatch(type, ans, std)) {
                    correct++;
                } else {
                    wrong++;
                }
            }
            int base = submittedCount;
            BigDecimal rate = base > 0
                    ? BigDecimal.valueOf(100.0 * correct / base).setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            Map<String, Object> one = new HashMap<>(q);
            one.put("index", idx++);
            one.put("correctCount", correct);
            one.put("wrongCount", wrong);
            one.put("blankCount", blank);
            one.put("submittedCount", base);
            one.put("correctRate", rate.doubleValue());
            one.put("notSubmittedCount", notSubmittedCount);
            questionStats.add(one);
        }

        List<Map<String, Object>> studentRows = new ArrayList<>();
        for (Map<String, Object> row : rawRecords) {
            Map<String, Object> s = new HashMap<>();
            s.put("studentId", row.get("studentId"));
            s.put("username", row.get("username"));
            s.put("nickname", row.get("nickname"));
            s.put("avatar", row.get("avatar"));
            s.put("status", row.get("status"));
            s.put("score", row.get("score"));
            s.put("submitTime", row.get("submitTime"));
            studentRows.add(s);
        }

        Map<String, Object> summary = new HashMap<>();
        summary.put("classSize", classSize);
        summary.put("submittedCount", submittedCount);
        summary.put("notSubmittedCount", notSubmittedCount);
        summary.put("averageScore", examEnded ? avg.doubleValue() : null);

        Map<String, Object> data = new HashMap<>();
        data.put("assignment", assign);
        data.put("summary", summary);
        data.put("students", studentRows);
        data.put("questions", questionStats);
        data.put("examEnded", examEnded);
        return Result.success(data);
    }

    /**
     * 老师 AI 自动组卷：题库优先，不足用 AI 生成自拟题补齐，最终落库为一张试卷模板（出现在模板库中）。
     * Body:
     * {
     *   "mcqCount": 10,
     *   "fillCount": 5,
     *   "codingCount": 1,
     *   "title": "可选自定义标题"
     * }
     */
    @PostMapping("/lms/assignment/{assignmentId}/ai-paper/generate")
    @Transactional
    public Result<Map<String, Object>> generateAiPaperFromAssignment(
            HttpServletRequest request,
            @PathVariable Long assignmentId,
            @RequestBody Map<String, Object> body
    ) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");

        Map<String, Object> assign = teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId);
        if (assign == null) return Result.error("考试任务不存在");

        LocalDateTime now = LocalDateTime.now(ZoneId.systemDefault());
        LocalDateTime start = toLocalDateTime(assign.get("startTime"));
        LocalDateTime end = toLocalDateTime(assign.get("endTime"));
        if (start != null && now.isBefore(start)) {
            return Result.error("考试尚未开始，暂不能基于本场考试自动组卷");
        }
        if (end != null && now.isBefore(end)) {
            return Result.error("考试未截止，暂不能基于本场考试自动组卷");
        }

        int mcqCount = parseInt(body.get("mcqCount"), 10, 0, 50);
        int fillCount = parseInt(body.get("fillCount"), 5, 0, 50);
        int codingCount = parseInt(body.get("codingCount"), 1, 0, 10);
        if (mcqCount + fillCount + codingCount <= 0) {
            return Result.error("题目数量不能全为 0");
        }

        Long paperIdSrc = assign.get("paperId") == null ? null : Long.valueOf(String.valueOf(assign.get("paperId")));
        String category = paperIdSrc == null ? null : teacherAdminMapper.getPaperCategory(paperIdSrc);
        if (category == null || category.isBlank()) category = "kmeans";

        // 1) 找薄弱点：按正确率从低到高取前 N 道（N 取 6，足够做提示/出题）
        List<Map<String, Object>> qStats = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);
        if (qStats == null || qStats.isEmpty()) {
            return Result.error("本场考试没有题目数据，不能自动组卷");
        }
        // 复用 analytics 里的计算：为了不重复走一遍，我们直接按“全班提交记录”重新算一遍正确率
        List<Map<String, Object>> rawRecords = teacherAdminMapper.listRecordsForAssignment(assignmentId);
        List<Map<String, Object>> submittedRows = new ArrayList<>();
        for (Map<String, Object> row : rawRecords) {
            int st = row.get("status") == null ? 0 : Integer.parseInt(row.get("status").toString());
            if (st >= 1) submittedRows.add(row);
        }
        if (submittedRows.isEmpty()) {
            return Result.error("本场考试暂无可分析的交卷数据，不能自动组卷");
        }

        List<Map<String, Object>> weakList = new ArrayList<>();
        for (Map<String, Object> q : qStats) {
            Long qid = Long.valueOf(String.valueOf(q.get("id")));
            String type = q.get("type") == null ? "" : String.valueOf(q.get("type"));
            String std = q.get("standardAnswer") == null ? null : String.valueOf(q.get("standardAnswer"));
            int correct = 0;
            int base = submittedRows.size();
            for (Map<String, Object> rec : submittedRows) {
                String json = rec.get("answersJson") == null ? null : rec.get("answersJson").toString();
                JsonNode root;
                try {
                    root = OBJECT_MAPPER.readTree(json == null || json.isBlank() ? "{}" : json);
                } catch (Exception e) {
                    root = OBJECT_MAPPER.createObjectNode();
                }
                String ans = readAnswerForQuestion(root, qid);
                if (!ans.isEmpty() && answersMatch(type, ans, std)) {
                    correct++;
                }
            }
            double rate = base > 0 ? (100.0 * correct / base) : 0.0;
            Map<String, Object> item = new HashMap<>();
            item.put("id", qid);
            item.put("type", type);
            item.put("content", q.get("content"));
            item.put("correctRate", rate);
            weakList.add(item);
        }
        weakList.sort((a, b) -> Double.compare(
                Double.parseDouble(String.valueOf(a.getOrDefault("correctRate", 0))),
                Double.parseDouble(String.valueOf(b.getOrDefault("correctRate", 0)))
        ));
        if (weakList.size() > 6) weakList = weakList.subList(0, 6);

        // 2) 题库优先：同模块按题型抽题（避免复用原试卷题目，减少“换皮重考”）
        List<Long> excludeIds = new ArrayList<>();
        for (Map<String, Object> q : qStats) {
            if (q.get("id") != null) excludeIds.add(Long.valueOf(String.valueOf(q.get("id"))));
        }
        List<Long> selectedQids = new ArrayList<>();


    
        List<Long> pickedMcq = pickFromBank(category, "选择题", "SINGLE_CHOICE", excludeIds, mcqCount);
        excludeIds.addAll(pickedMcq);
        List<Long> pickedFill = pickFromBank(category, "填空题", "FILL_BLANK", excludeIds, fillCount);
        excludeIds.addAll(pickedFill);
        List<Long> pickedCoding = pickFromBank(category, "编程题", "CODING", excludeIds, codingCount);
        excludeIds.addAll(pickedCoding);

        selectedQids.addAll(pickedMcq);
        selectedQids.addAll(pickedFill);
        selectedQids.addAll(pickedCoding);

        int needMcq = Math.max(0, mcqCount - pickedMcq.size());
        int needFill = Math.max(0, fillCount - pickedFill.size());
        int needCoding = Math.max(0, codingCount - pickedCoding.size());

        // 用 AI 生成自拟题补齐（生成后写入 sys_question，再加入试卷）
        List<Long> generatedQids = new ArrayList<>();
        boolean aiInvoked = true;
        long aiCostMs = 0L;
        String aiError = null;
        if (needMcq + needFill + needCoding > 0) {
            String assignName = assign.get("publishName") == null ? "" : String.valueOf(assign.get("publishName"));
            String prompt = buildTeacherPaperPrompt(category, assignName, weakList, needMcq, needFill, needCoding);
            try {
                long aiStart = System.currentTimeMillis();
                String raw = teacherPaperClient.prompt().user(prompt).call().content();
                aiCostMs = Math.max(0, System.currentTimeMillis() - aiStart);
                //bug修复：把json中的```json和```去掉   
                String clean = raw.replace("```json", "").replace("```", "").trim();
                JsonNode root = OBJECT_MAPPER.readTree(clean);
                String genTitle = root.path("title").asText("");
                JsonNode questionsNode = root.path("questions");
                if (questionsNode.isArray()) {
                    for (JsonNode qn : questionsNode) {
                        String t = qn.path("type").asText("").trim();
                        String content = qn.path("content").asText("").trim();
                        if (content.isEmpty() || t.isEmpty()) continue;

                        List<String> opts = new ArrayList<>();
                        if ("选择题".equals(t) && qn.has("options") && qn.get("options").isArray()) {
                            for (JsonNode on : qn.get("options")) {
                                String ov = on.asText("").trim();
                                if (!ov.isEmpty()) opts.add(ov);
                            }
                            if (opts.size() != 4) continue; // 强约束：必须 4 选项
                        }
                        String std = qn.path("standardAnswer").asText("");
                        if (!"编程题".equals(t) && (std == null || std.trim().isEmpty())) continue;

                        Map<String, Object> row = new HashMap<>();
                        row.put("teacherId", teacherId);
                        row.put("category", category);
                        row.put("type", t);
                        row.put("content", content);
                        row.put("optionsJson", OBJECT_MAPPER.writeValueAsString(opts));
                        row.put("standardAnswer", (std == null || std.isBlank()) ? " " : std);
                        teacherAdminMapper.insertTeacherQuestion(row);
                        Object newIdObj = row.get("id");
                        if (newIdObj != null) {
                            generatedQids.add(Long.valueOf(String.valueOf(newIdObj)));
                        }
                    }
                }
            } catch (Exception e) {
                aiError = e.getMessage();
                return Result.error("AI 出题失败：" + (aiError == null ? "未知错误" : aiError));
            }
        }

        // 创建试卷模板并挂题
        String customTitle = body.get("title") == null ? "" : String.valueOf(body.get("title")).trim();
        String title = !customTitle.isEmpty()
                ? customTitle
                : buildDefaultPaperTitle(category);

        Map<String, Object> paperRow = new HashMap<>();
        paperRow.put("teacherId", teacherId);
        paperRow.put("title", title);
        paperRow.put("category", category);
        teacherAdminMapper.insertPaper(paperRow);
        Long newPaperId = Long.valueOf(String.valueOf(paperRow.get("id")));

        for (Long qid : selectedQids) {
            teacherAdminMapper.insertPaperQuestion(newPaperId, qid);
        }
        for (Long qid : generatedQids) {
            teacherAdminMapper.insertPaperQuestion(newPaperId, qid);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("paperId", newPaperId);
        out.put("title", title);
        out.put("category", category);
        out.put("pickedFromBank", selectedQids.size());
        out.put("generated", generatedQids.size());
        out.put("aiInvoked", aiInvoked);
        out.put("aiCostMs", aiCostMs);
        out.put("aiSkippedReason", "");
        out.put("weakTop", weakList);
        return Result.success(out);
    }

    private int parseInt(Object v, int def, int min, int max) {
        int x = def;
        if (v != null) {
            try { x = Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) { x = def; }
        }
        if (x < min) x = min;
        if (x > max) x = max;
        return x;
    }

    private List<Long> pickFromBank(String category, String cnType, String enumType, List<Long> exclude, int limit) {
        if (limit <= 0) return new ArrayList<>();
        List<Map<String, Object>> rows = teacherAdminMapper.pickQuestionsForAiPaper(category, cnType, exclude, limit);
        if (rows == null || rows.isEmpty()) {
            rows = teacherAdminMapper.pickQuestionsForAiPaper(category, enumType, exclude, limit);
        }
        List<Long> ids = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            if (r.get("id") != null) {
                try { ids.add(Long.valueOf(String.valueOf(r.get("id")))); } catch (Exception ignored) {}
            }
        }
        return ids;
    }

    private String buildTeacherPaperPrompt(String category, String assignmentName, List<Map<String, Object>> weakTop,
                                          int mcqNeed, int fillNeed, int codingNeed) {
        StringBuilder sb = new StringBuilder();
        sb.append("模块：").append(category).append("\n");
        if (assignmentName != null && !assignmentName.isBlank()) sb.append("参考考试：").append(assignmentName).append("\n");
        sb.append("班级薄弱题（低正确率优先）：\n");
        for (Map<String, Object> w : weakTop) {
            sb.append("- 正确率 ").append(String.format("%.1f", Double.parseDouble(String.valueOf(w.getOrDefault("correctRate", 0)))))
              .append("% | ").append(String.valueOf(w.getOrDefault("type", "")))
              .append(" | ").append(String.valueOf(w.getOrDefault("content", ""))).append("\n");
        }
        sb.append("\n请生成：选择题 ").append(mcqNeed).append(" 道，填空题 ").append(fillNeed).append(" 道，编程题 ").append(codingNeed).append(" 道。\n");
        sb.append("注意：选择题必须 4 个选项，且标准答案为 A/B/C/D。\n");
        return sb.toString();
    }

    private String buildDefaultPaperTitle(String category) {
        String md = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        String module;
        if ("kmeans".equalsIgnoreCase(category)) module = "K-Means";
        else if ("linear".equalsIgnoreCase(category)) module = "线性回归";
        else if ("logistic".equalsIgnoreCase(category)) module = "逻辑回归";
        else if ("neural".equalsIgnoreCase(category)) module = "神经网络";
        else if ("text".equalsIgnoreCase(category)) module = "文本分类";
        else module = (category == null || category.isBlank()) ? "算法" : category;
        return module + " 练习卷 " + md;
    }

    /** 已交卷学生列表（用于教师逐份批改入口） */
    @GetMapping("/lms/assignment/{assignmentId}/submissions")
    public Result<List<Map<String, Object>>> assignmentSubmissions(HttpServletRequest request, @PathVariable Long assignmentId) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        Map<String, Object> assign = teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId);
        if (assign == null) return Result.error("考试任务不存在");

        List<Map<String, Object>> rows = teacherAdminMapper.listRecordsForAssignment(assignmentId);
        List<Map<String, Object>> submitted = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            int st = row.get("status") == null ? 0 : Integer.parseInt(String.valueOf(row.get("status")));
            if (st >= 1) {
                submitted.add(row);
            }
        }
        return Result.success(submitted);
    }

    /** 查看某个学生该场考试的整份答卷详情（老师批改用） */
    @GetMapping("/lms/assignment/{assignmentId}/submission/{studentId}")
    public Result<Map<String, Object>> submissionDetail(
            HttpServletRequest request,
            @PathVariable Long assignmentId,
            @PathVariable Long studentId
    ) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        Map<String, Object> assign = teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId);
        if (assign == null) return Result.error("考试任务不存在");

        Map<String, Object> examTitleRow = studentExamMapper.getExamDetail(assignmentId);
        if (examTitleRow != null && examTitleRow.get("paperTitle") != null) {
            assign = new HashMap<>(assign);
            assign.put("paperTitle", examTitleRow.get("paperTitle"));
        }

        Map<String, Object> rec = teacherAdminMapper.getSubmissionRecord(assignmentId, studentId);
        if (rec == null) return Result.error("该学生暂无答卷");
        int st = rec.get("status") == null ? 0 : Integer.parseInt(String.valueOf(rec.get("status")));
        if (st < 1) return Result.error("该学生尚未交卷");

        JsonNode answerRoot;
        try {
            String j = rec.get("answersJson") == null ? "{}" : String.valueOf(rec.get("answersJson"));
            answerRoot = OBJECT_MAPPER.readTree(j.isBlank() ? "{}" : j);
        } catch (Exception e) {
            answerRoot = OBJECT_MAPPER.createObjectNode();
        }

        List<Map<String, Object>> questions = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);
        List<Map<String, Object>> details = new ArrayList<>();
        int idx = 1;
        for (Map<String, Object> q : questions) {
            Long qid = Long.valueOf(String.valueOf(q.get("id")));
            int fullScore = q.get("score") == null ? 0 : Integer.parseInt(String.valueOf(q.get("score")));
            String studentAns = readAnswerForQuestion(answerRoot, qid);
            String std = q.get("standardAnswer") == null ? "" : String.valueOf(q.get("standardAnswer"));
            boolean matched = answersMatch(String.valueOf(q.get("type")), studentAns, std);

            Map<String, Object> item = new HashMap<>(q);
            item.put("index", idx++);
            item.put("studentAnswer", studentAns);
            item.put("fullScore", fullScore);
            item.put("suggestScore", matched ? fullScore : 0);
            details.add(item);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("assignment", assign);
        data.put("student", rec);
        data.put("questions", details);
        return Result.success(data);
    }

    /** 老师重评分：按每题给分，回写总分（学生端与统计会自动同步） */
    @PostMapping("/lms/assignment/{assignmentId}/submission/{studentId}/rescore")
    @Transactional
    public Result<Map<String, Object>> rescoreSubmission(
            HttpServletRequest request,
            @PathVariable Long assignmentId,
            @PathVariable Long studentId,
            @RequestBody Map<String, Object> body
    ) {
        if (!isAdmin(request)) return Result.error("未授权访问");
        Long teacherId = getUserIdFromToken(request);
        if (teacherId == null) return Result.error("未授权访问");
        if (teacherAdminMapper.getAssignmentForTeacher(assignmentId, teacherId) == null) {
            return Result.error("考试任务不存在");
        }

        Map<String, Object> rec = teacherAdminMapper.getSubmissionRecord(assignmentId, studentId);
        if (rec == null) return Result.error("该学生暂无答卷");
        int st = rec.get("status") == null ? 0 : Integer.parseInt(String.valueOf(rec.get("status")));
        if (st < 1) return Result.error("该学生尚未交卷");

        Object scoresObj = body.get("questionScores");
        if (!(scoresObj instanceof Map<?, ?> scoreMapRaw)) {
            return Result.error("缺少 questionScores");
        }

        JsonNode answerRoot;
        try {
            String j = rec.get("answersJson") == null ? "{}" : String.valueOf(rec.get("answersJson"));
            answerRoot = OBJECT_MAPPER.readTree(j.isBlank() ? "{}" : j);
        } catch (Exception e) {
            answerRoot = OBJECT_MAPPER.createObjectNode();
        }

        List<Map<String, Object>> questions = studentExamMapper.listExamQuestionsWithAnswer(assignmentId);
        int total = 0;
        for (Map<String, Object> q : questions) {
            Long qid = Long.valueOf(String.valueOf(q.get("id")));
            int full = q.get("score") == null ? 0 : Integer.parseInt(String.valueOf(q.get("score")));
            String qType = q.get("type") == null ? "" : String.valueOf(q.get("type"));
            int sc;
            // 选择题不允许人工改分：固定按标准答案自动判分
            if ("选择题".equals(qType)) {
                String studentAns = readAnswerForQuestion(answerRoot, qid);
                String std = q.get("standardAnswer") == null ? "" : String.valueOf(q.get("standardAnswer"));
                sc = answersMatch(qType, studentAns, std) ? full : 0;
            } else {
                Object scObj = scoreMapRaw.get(String.valueOf(qid));
                if (scObj == null) scObj = scoreMapRaw.get(qid);
                sc = 0;
                if (scObj != null) {
                    try {
                        sc = Integer.parseInt(String.valueOf(scObj));
                    } catch (Exception ignored) {
                        sc = 0;
                    }
                }
            }
            if (sc < 0) sc = 0;
            if (sc > full) sc = full;
            total += sc;
        }

        teacherAdminMapper.updateSubmissionScore(assignmentId, studentId, total);
        Map<String, Object> data = new HashMap<>();
        data.put("score", total);
        return Result.success(data);
    }

    private static LocalDateTime toLocalDateTime(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (o instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (o instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }

    private static String readAnswerForQuestion(JsonNode answersRoot, Long qid) {
        if (answersRoot == null || !answersRoot.isObject()) {
            return "";
        }
        JsonNode n = answersRoot.get(String.valueOf(qid));
        if (n == null || n.isNull()) {
            return "";
        }
        if (n.isTextual()) {
            return n.asText().trim();
        }
        if (n.isNumber()) {
            return n.asText().trim();
        }
        return n.toString().trim();
    }

    private static boolean answersMatch(String type, String studentAns, String standard) {
        if (standard == null) {
            return false;
        }
        String s = studentAns == null ? "" : studentAns.trim();
        if (s.isEmpty()) {
            return false;
        }
        return standard.trim().equalsIgnoreCase(s);
    }
}