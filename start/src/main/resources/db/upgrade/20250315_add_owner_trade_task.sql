--
-- 表的结构 `owner_trade_task`
--

CREATE TABLE IF NOT EXISTS `owner_trade_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '任务ID',
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所有者',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息ID',
  `task_type` int NOT NULL COMMENT '任务类型',
  `status` int NOT NULL COMMENT '任务状态',
  `market` int DEFAULT NULL COMMENT '任务标的市场',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '任务标的代码',
  `strategy_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '策略ID',
  `start_time` datetime NOT NULL COMMENT '任务开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '任务结束时间',
  `create_time` datetime NOT NULL COMMENT '任务创建时间',
  `update_time` datetime NOT NULL COMMENT '任务更新时间',
  `ext` json DEFAULT NULL COMMENT '扩展信息',
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_message_id` (`message_id`),
  KEY `idx_status` (`status`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='交易任务表';