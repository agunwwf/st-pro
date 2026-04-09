CREATE DATABASE IF NOT EXISTS apple_admin DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS `sys_check_in` (
                                              `id` bigint NOT NULL AUTO_INCREMENT,
                                              `user_id` bigint NOT NULL COMMENT '用户ID',
                                              `check_date` date NOT NULL COMMENT '打卡日期',
                                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                              PRIMARY KEY (`id`),
                                              UNIQUE KEY `uk_user_date` (`user_id`,`check_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sys_friend` (
                                            `id` bigint NOT NULL AUTO_INCREMENT,
                                            `user_id` bigint NOT NULL COMMENT '用户ID',
                                            `friend_id` bigint NOT NULL COMMENT '好友用户ID',
                                            `status` tinyint DEFAULT '0' COMMENT '0:申请中, 1:已同意, 2:已拒绝',
                                            `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                            PRIMARY KEY (`id`),
                                            UNIQUE KEY `uk_relation` (`user_id`,`friend_id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sys_group` (
                                           `id` bigint NOT NULL AUTO_INCREMENT,
                                           `name` varchar(100) NOT NULL COMMENT '群名称',
                                           `avatar` varchar(255) DEFAULT NULL COMMENT '群头像',
                                           `owner_id` bigint NOT NULL COMMENT '群主ID',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sys_group_user` (
                                                `id` bigint NOT NULL AUTO_INCREMENT,
                                                `group_id` bigint NOT NULL COMMENT '群ID',
                                                `user_id` bigint NOT NULL COMMENT '用户ID',
                                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                                PRIMARY KEY (`id`),
                                                UNIQUE KEY `uk_group_user` (`group_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sys_learning_thread` (
                                                     `id` bigint NOT NULL AUTO_INCREMENT,
                                                     `user_id` bigint NOT NULL COMMENT '用户ID',
                                                     `title` varchar(100) NOT NULL COMMENT '进度标题',
                                                     `content` text COMMENT '详细内容',
                                                     `duration` int DEFAULT '0' COMMENT '学习时长(分钟)',
                                                     `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                                     PRIMARY KEY (`id`),
                                                     KEY `idx_user_time` (`user_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sys_message` (
                                             `id` bigint NOT NULL AUTO_INCREMENT,
                                             `from_user` varchar(50) NOT NULL COMMENT '发送者用户名',
                                             `to_user` varchar(50) NOT NULL COMMENT '接收者用户名',
                                             `content` longtext COMMENT '消息内容 (文本或 Base64 图片)',
                                             `file_name` varchar(255) DEFAULT NULL COMMENT '文件名',
                                             `msg_type` varchar(20) DEFAULT 'text' COMMENT 'text/image/file',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (`id`),
                                             KEY `idx_from_to` (`from_user`,`to_user`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE IF NOT EXISTS `sys_user` (
                                          `id` bigint NOT NULL AUTO_INCREMENT,
                                          `username` varchar(50) NOT NULL COMMENT '用户名',
                                          `password` varchar(100) NOT NULL COMMENT '密码',
                                          `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
                                          `avatar` longtext COMMENT '头像(Base64)',
                                          `signature` varchar(255) DEFAULT NULL COMMENT '个性签名',
                                          `role` varchar(20) DEFAULT 'STUDENT' COMMENT '角色: ADMIN/STUDENT',
                                          `teacher_id` bigint DEFAULT NULL COMMENT '绑定的指导老师ID (仅对STUDENT生效)',
                                          `gender` varchar(16) DEFAULT NULL COMMENT '性别: 男/女/保密',
                                          `birthday` date DEFAULT NULL COMMENT '生日',
                                          `region` varchar(64) DEFAULT NULL COMMENT '地区',
                                          `progress` int DEFAULT '0' COMMENT '学习进度(0-100)',
                                          `check_in_count` int DEFAULT '0' COMMENT '打卡次数',
                                          `is_model` tinyint(1) DEFAULT '0' COMMENT '是否为模范学生',
                                          `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_username` (`username`),
                                          KEY `idx_teacher_id` (`teacher_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
-- 论坛帖子主表
CREATE TABLE IF NOT EXISTS `sys_forum_post` (
                                                `id` bigint NOT NULL AUTO_INCREMENT,
                                                `user_id` bigint NOT NULL COMMENT '作者ID (关联sys_user.id)',
                                                `title` varchar(100) NOT NULL COMMENT '标题',
                                                `content` text NOT NULL COMMENT '正文',
                                                `section` varchar(20) NOT NULL COMMENT '板块: knowledge/q-a/help/notes',
                                                `cover` varchar(255) DEFAULT NULL COMMENT '封面图URL',
                                                `votes` int DEFAULT '0' COMMENT '点赞数',
                                                `stars` int DEFAULT '0' COMMENT '收藏数',
                                                `comments` int DEFAULT '0' COMMENT '评论数',
                                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                                PRIMARY KEY (`id`),
                                                KEY `idx_user_id` (`user_id`),
                                                KEY `idx_section` (`section`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


CREATE TABLE IF NOT EXISTS `sys_forum_action` (
                                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                                  `user_id` bigint NOT NULL,
                                                  `post_id` bigint NOT NULL,
                                                  `action_type` varchar(10) NOT NULL COMMENT 'vote/star',
                                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                                  PRIMARY KEY (`id`),
                                                  UNIQUE KEY `uk_user_post_action` (`user_id`, `post_id`, `action_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 论坛帖子浏览记录（登录用户打开详情页时写入/更新最后浏览时间）
CREATE TABLE IF NOT EXISTS `sys_forum_post_view` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `post_id` bigint NOT NULL,
    `last_view_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_post_view` (`user_id`, `post_id`),
    KEY `idx_user_last_view` (`user_id`, `last_view_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户浏览帖子记录';

-- 论坛帖子评论
CREATE TABLE IF NOT EXISTS `sys_forum_comment` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `post_id` bigint NOT NULL,
    `user_id` bigint NOT NULL,
    `content` varchar(2000) NOT NULL,
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_forum_comment_post` (`post_id`),
    KEY `idx_forum_comment_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='论坛评论';

-- ---------------------------------------------------------------------------
-- 学习完成记录表：谁在什么时间标记完成了哪个项目的哪种学习。
-- · user_id：关联 sys_user，一次登录一个用户。
-- · module_id：与前端/Streamlit 约定的项目代号（5 个 ML 实验）。
-- · kind：demo = 浏览完「演示教学」侧边栏点的完成；step = 「分步练习」最后一页点的完成。
-- · uk_user_module_kind：同一用户对同一 module + kind 只能有一条 → 重复点击「已完成」不会多插行。
-- 理论最多记录数 = 用户数 × 5(module) × 2(kind)；Dashboard 的「已完成教学」用 COUNT 展示进度。
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_learning_completion` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `user_id` bigint NOT NULL,
    `module_id` varchar(32) NOT NULL COMMENT 'kmeans, logistic, neural, linear, text',
    `kind` varchar(16) NOT NULL COMMENT 'demo | step',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_module_kind` (`user_id`, `module_id`, `kind`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 学生成绩表 （后续可以在后台管理系统中做“成绩统计图表”）

CREATE TABLE IF NOT EXISTS `sys_quiz_score` (
                                  `id` bigint NOT NULL AUTO_INCREMENT,
                                  `user_id` bigint NOT NULL COMMENT '用户ID',
                                  `module_id` varchar(32) NOT NULL COMMENT '模块代号 (如: kmeans, linear, neural)',
                                  `score` int NOT NULL COMMENT '测验得分',
                                  `answers_detail` JSON NOT NULL COMMENT '详尽答题病历(供Vue端AI分析)',
                                  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (`id`),
    -- 同一个用户，同一个模块，在数据库里只允许有一条记录！
                                  UNIQUE KEY `uk_user_module` (`user_id`, `module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生测验成绩与病历表';

CREATE TABLE IF NOT EXISTS `sys_ai_chat` (
                                             `id` bigint NOT NULL AUTO_INCREMENT,
                                             `user_id` bigint NOT NULL COMMENT '用户ID',
                                             `role` varchar(20) NOT NULL COMMENT '角色: user 或 assistant',
                                             `content` text NOT NULL COMMENT '消息内容',
                                             `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                             PRIMARY KEY (`id`),
                                             KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `sys_ai_quiz_record` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                                    `user_id` bigint NOT NULL COMMENT '用户ID',
                                                    `module_id` varchar(32) NOT NULL COMMENT '关联模块',
                                                    `title` varchar(100) NOT NULL COMMENT '练习标题',
                                                    `weakness_analysis` text COMMENT 'AI弱点诊断',
                                                    `quiz_json` JSON NOT NULL COMMENT 'AI生成的题目',
                                                    `user_answers` JSON DEFAULT NULL COMMENT '用户答题',
                                                    `score` int DEFAULT NULL COMMENT '得分',
                                                    `status` tinyint DEFAULT '0' COMMENT '0-未完成, 1-已提交',
                                                    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                                    PRIMARY KEY (`id`),
                                                    KEY `idx_user_module` (`user_id`, `module_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- 1. 题库表 (sys_question) —— 存放所有的原始题目
CREATE TABLE IF NOT EXISTS `sys_question` (
                                              `id` bigint NOT NULL AUTO_INCREMENT,
                                              `teacher_id` bigint DEFAULT NULL COMMENT '谁出的题(NULL为系统自带)',
                                              `category` varchar(50) NOT NULL COMMENT '模块分类(kmeans等)',
                                              `type` varchar(20) NOT NULL COMMENT '题型: SINGLE_CHOICE/FILL_BLANK/CODING',
                                              `content` text NOT NULL COMMENT '题干',
                                              `options` JSON DEFAULT NULL COMMENT '选项数据',
                                              `standard_answer` text NOT NULL COMMENT '标准答案',
                                              `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                              PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 2. 试卷表 (sys_paper) —— 核心新增！固定不变的卷子模板
CREATE TABLE IF NOT EXISTS `sys_paper` (
                                           `id` bigint NOT NULL AUTO_INCREMENT,
                                           `teacher_id` bigint NOT NULL COMMENT '组卷老师',
                                           `title` varchar(100) NOT NULL COMMENT '试卷名称 (例如: K-Means 2026标准测试卷A)',
                                           `category` varchar(50) NOT NULL COMMENT '所属模块',
                                           `total_score` int DEFAULT '100' COMMENT '卷面总分',
                                           `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                           PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 3. 试卷-题目关联表 (sys_paper_question) —— 这张卷子选了哪几道题
CREATE TABLE IF NOT EXISTS `sys_paper_question` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                                    `paper_id` bigint NOT NULL COMMENT '试卷ID',
                                                    `question_id` bigint NOT NULL COMMENT '题库题目ID',
                                                    `score` int DEFAULT '10' COMMENT '这道题在这张试卷里占几分',
                                                    PRIMARY KEY (`id`),
                                                    UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 4. 考试发布表 (sys_assignment) —— 老师将卷子发给学生的行为
CREATE TABLE IF NOT EXISTS `sys_assignment` (
                                                `id` bigint NOT NULL AUTO_INCREMENT,
                                                `teacher_id` bigint NOT NULL,
                                                `paper_id` bigint NOT NULL COMMENT '引用的试卷ID (灵魂解耦！)',
                                                `publish_name` varchar(100) NOT NULL COMMENT '发布名称 (例如: 计科一班周测)',
                                                `start_time` datetime NOT NULL COMMENT '开放时间',
                                                `end_time` datetime NOT NULL COMMENT '截止时间',
                                                `time_limit_minutes` int NOT NULL COMMENT '限时',
                                                `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                                PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;


-- 5. 学生答卷记录表 (sys_student_record) —— 考试快照
CREATE TABLE IF NOT EXISTS `sys_student_record` (
                                                    `id` bigint NOT NULL AUTO_INCREMENT,
                                                    `assignment_id` bigint NOT NULL COMMENT '对应的考试任务ID',
                                                    `student_id` bigint NOT NULL COMMENT '学生ID',
                                                    `status` tinyint DEFAULT '0' COMMENT '0:未交, 1:待批, 2:已批',
                                                    `score` int DEFAULT '0' COMMENT '得分',
                                                    `student_answers` JSON DEFAULT NULL COMMENT '答题详情快照',
                                                    `submit_time` datetime DEFAULT NULL,
                                                    PRIMARY KEY (`id`),
                                                    UNIQUE KEY `uk_student_assign` (`student_id`, `assignment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 班级关系申请表：学生申请加入/更换导师（需要审批）
-- 规则：
-- - BIND：学生未绑定导师时，申请加入 new_teacher_id；需要老师同意
-- - SWITCH：学生已绑定 old_teacher_id，申请更换到 new_teacher_id；需要 两个老师 都同意
-- - 同一学生对同一 new_teacher 同类型只允许存在一条 
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_teacher_student_request` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `student_id` bigint NOT NULL,
    `old_teacher_id` bigint DEFAULT NULL,
    `new_teacher_id` bigint NOT NULL,
    `req_type` varchar(16) NOT NULL COMMENT 'BIND | SWITCH',
    `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING | APPROVED | REJECTED | CANCELLED',
    `approve_old` tinyint(1) NOT NULL DEFAULT '0',
    `approve_new` tinyint(1) NOT NULL DEFAULT '0',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_req_student` (`student_id`),
    KEY `idx_req_old_teacher` (`old_teacher_id`),
    KEY `idx_req_new_teacher` (`new_teacher_id`),
    KEY `idx_req_status` (`status`),
    UNIQUE KEY `uk_req_pending` (`student_id`, `new_teacher_id`, `req_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- 班级拉黑表：老师踢出学生后，学生不可再次申请加入该老师
-- 只有老师手动添加学生才允许（同时解除拉黑）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `sys_teacher_student_block` (
    `id` bigint NOT NULL AUTO_INCREMENT,
    `teacher_id` bigint NOT NULL,
    `student_id` bigint NOT NULL,
    `reason` varchar(255) DEFAULT NULL,
    `active` tinyint(1) NOT NULL DEFAULT '1',
    `created_by` bigint NOT NULL COMMENT '执行踢出/拉黑的老师ID',
    `blocked_at` datetime DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_block_teacher_student` (`teacher_id`, `student_id`),
    KEY `idx_block_teacher` (`teacher_id`),
    KEY `idx_block_student` (`student_id`),
    KEY `idx_block_active` (`active`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;