-- ----------------------------
-- 1. 角色表 (sys_role)
-- ----------------------------
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色主键',
  `role_name` varchar(30) NOT NULL COMMENT '角色名称 (如：超级管理员)',
  `role_key` varchar(50) NOT NULL COMMENT '角色权限标识 (如：admin)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1正常 0停用）',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除（0未删除 1已删除）',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

-- 初始化必须的基础角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `remark`) VALUES (1, '超级管理员', 'admin', '系统最高权限');
INSERT INTO `sys_role` (`id`, `role_name`, `role_key`, `remark`) VALUES (2, '普通用户', 'common', 'C端注册用户');

-- ----------------------------
-- 2. 用户表 (sys_user)
-- ----------------------------
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` varchar(50) NOT NULL COMMENT '登录账号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '用户昵称',
  `avatar` varchar(255) DEFAULT '' COMMENT '头像URL',
  `email` varchar(100) DEFAULT '' COMMENT '邮箱',
  `phone` varchar(20) DEFAULT '' COMMENT '手机号',
  
  -- AIGC 核心业务资产
  `points` int NOT NULL DEFAULT '0' COMMENT '音乐积分余额 (生成音乐扣除)',
  
  -- 安全与权限控制
  `role_id` bigint NOT NULL DEFAULT '2' COMMENT '角色ID (默认关联普通用户)',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '帐号状态（1正常 0封禁）',
  `is_deleted` tinyint NOT NULL DEFAULT '0' COMMENT '逻辑删除标识（0未删除 1已删除）',
  
  -- 审计与追踪
  `last_login_ip` varchar(50) DEFAULT '' COMMENT '最后登录IP',
  `last_login_time` datetime DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`), -- 数据库层面兜底，保证账号全局唯一不重复
  KEY `idx_role_id` (`role_id`),
  CONSTRAINT `fk_sys_user_role_id` FOREIGN KEY (`role_id`) REFERENCES `sys_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ----------------------------
-- 3. 音乐任务表 (music_task)
-- ----------------------------
CREATE TABLE `music_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  `task_id` varchar(64) NOT NULL COMMENT '任务编号（对外展示）',
  `vendor_task_id` varchar(128) DEFAULT NULL COMMENT '第三方任务号（如 SunoAPI 返回的 taskId，用于 record-info 轮询）',
  `user_id` bigint NOT NULL COMMENT '发起用户ID',
  `model_code` varchar(64) NOT NULL COMMENT '模型标识（如：melodify-v1）',
  `prompt` text COMMENT '用户输入提示词',
  `params` json DEFAULT NULL COMMENT '结构化生成参数（风格/BPM/时长等）',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '任务状态（0排队 1生成中 2成功 3失败 4取消）',
  `error_code` varchar(64) DEFAULT '' COMMENT '失败错误码',
  `error_message` varchar(500) DEFAULT '' COMMENT '失败描述',
  `cost_points` int NOT NULL DEFAULT '0' COMMENT '任务消耗积分',
  `started_at` datetime DEFAULT NULL COMMENT '开始生成时间',
  `finished_at` datetime DEFAULT NULL COMMENT '任务结束时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_task_id` (`task_id`),
  KEY `idx_vendor_task_id` (`vendor_task_id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_create_time` (`create_time`),
  CONSTRAINT `fk_music_task_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音乐任务表';

-- ----------------------------
-- 4. 音乐资产表 (music_asset)
-- ----------------------------
CREATE TABLE `music_asset` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '资产主键',
  `asset_id` varchar(64) NOT NULL COMMENT '资产编号',
  `task_id` bigint NOT NULL COMMENT '来源任务ID',
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `title` varchar(120) DEFAULT '' COMMENT '音乐标题',
  `file_url` varchar(500) NOT NULL COMMENT '音频文件URL',
  `cover_url` varchar(500) DEFAULT '' COMMENT '封面URL',
  `duration_sec` int NOT NULL DEFAULT '0' COMMENT '时长（秒）',
  `format` varchar(20) NOT NULL DEFAULT 'mp3' COMMENT '音频格式',
  `bitrate_kbps` int NOT NULL DEFAULT '320' COMMENT '码率',
  `is_public` tinyint NOT NULL DEFAULT '0' COMMENT '是否公开（0私有 1公开）',
  `license_type` varchar(30) NOT NULL DEFAULT 'personal' COMMENT '授权类型（personal/commercial）',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态（1有效 0失效）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_id` (`asset_id`),
  KEY `idx_user_create_time` (`user_id`, `create_time`),
  KEY `idx_task_id` (`task_id`),
  CONSTRAINT `fk_music_asset_task_id` FOREIGN KEY (`task_id`) REFERENCES `music_task` (`id`),
  CONSTRAINT `fk_asset_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='音乐资产表';

-- ----------------------------
-- 5. 积分日志表 (point_log)
-- ----------------------------
CREATE TABLE `point_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水主键',
  `log_id` varchar(64) NOT NULL COMMENT '日志编号',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `change_type` tinyint NOT NULL COMMENT '变动类型（1充值 2消费 3退款 4活动发放 5管理员调整）',
  `amount` int NOT NULL COMMENT '变动积分（正数加、负数减）',
  `balance` int NOT NULL COMMENT '变动后余额',
  `biz_type` varchar(30) NOT NULL DEFAULT '' COMMENT '业务类型（generate/refund/recharge）',
  `biz_id` varchar(64) NOT NULL DEFAULT '' COMMENT '关联业务ID（任务号/订单号）',
  `remark` varchar(255) DEFAULT '' COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_log_id` (`log_id`),
  KEY `idx_user_create_time` (`user_id`, `create_time`),
  KEY `idx_biz_type_biz_id` (`biz_type`, `biz_id`),
  CONSTRAINT `fk_point_log_user_id` FOREIGN KEY (`user_id`) REFERENCES `sys_user` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分日志表';

-- ---------------------------------------------------------------------------
-- 以下为「已有库增量」备忘：若在增加 vendor_task_id 字段前已初始化过库，按需执行：
-- ALTER TABLE `music_task` ADD COLUMN `vendor_task_id` varchar(128) DEFAULT NULL COMMENT '第三方任务号（SunoAPI 等）' AFTER `task_id`;
-- ALTER TABLE `music_task` ADD KEY `idx_vendor_task_id` (`vendor_task_id`);
-- ---------------------------------------------------------------------------