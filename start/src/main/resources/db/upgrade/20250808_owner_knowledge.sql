CREATE TABLE `owner_knowledge` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `owner` varchar(50) NOT NULL COMMENT '所有者',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `type` varchar(50) NOT NULL COMMENT '类型（OPTION_STRATEGY: 期权策略知识, RULE: 规则知识）',
  `description` varchar(1000) DEFAULT NULL COMMENT '描述',
  `content` text COMMENT '内容（支持Markdown）',
  `status` int(11) NOT NULL DEFAULT '1' COMMENT '状态（1: 启用, 0: 禁用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner` (`owner`),
  KEY `idx_type` (`type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='所有者知识库表';