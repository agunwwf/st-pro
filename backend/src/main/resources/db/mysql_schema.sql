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
                                          `progress` int DEFAULT '0' COMMENT '学习进度(0-100)',
                                          `check_in_count` int DEFAULT '0' COMMENT '打卡次数',
                                          `is_model` tinyint(1) DEFAULT '0' COMMENT '是否为模范学生',
                                          `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
                                          PRIMARY KEY (`id`),
                                          UNIQUE KEY `uk_username` (`username`)
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
    `module_id` varchar(32) NOT NULL COMMENT '模块代号，例如: kmeans',
    `score` int NOT NULL COMMENT '测验得分',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
    `answers_detail` JSON COMMENT '学生的具体答题详情(用于后台错题分析)',
    PRIMARY KEY (`id`),
    KEY `idx_user_module` (`user_id`, `module_id`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测验成绩表';