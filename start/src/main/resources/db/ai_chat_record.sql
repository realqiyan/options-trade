-- 表的结构 `ai_chat_record`
--

CREATE TABLE IF NOT EXISTS `ai_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '所有者',
  `session_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `message_id` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息ID',
  `role` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型（user: 用户消息, assistant: 助手消息）',
  `content` text COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `title` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话标题（股票+策略）',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner_session` (`owner`, `session_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI助手沟通记录表';

-- 添加表注释
ALTER TABLE `ai_chat_record` COMMENT='AI助手沟通记录表';
