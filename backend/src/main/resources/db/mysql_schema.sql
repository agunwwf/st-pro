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