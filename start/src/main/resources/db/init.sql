-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- 主机： 10.0.12.220
-- 生成日期： 2025-03-08 07:00:26
-- 服务器版本： 8.2.0
-- PHP 版本： 8.2.27

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- 数据库： `options`
--

-- --------------------------------------------------------

--
-- 表的结构 `owner_account`
--

CREATE TABLE IF NOT EXISTS `owner_account` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一用户名',
  `platform` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '平台',
  `market` int NOT NULL COMMENT '市场',
  `account_id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
  `otp_auth` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'otp认证信息',
  `status` int NOT NULL COMMENT '状态 1有效 0无效',
  `ext` json DEFAULT NULL COMMENT '扩展配置，存储额外的配置信息，只支持一层配置，且只支持字符串类型',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner` (`owner`),
  UNIQUE KEY `uk_platform_market_account` (`platform`,`account_id`,`market`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账号表';

--
-- 转存表中的数据 `owner_account`
--

INSERT INTO `owner_account` (`id`, `create_time`, `owner`, `platform`, `market`, `account_id`, `otp_auth`, `status`, `ext`) VALUES
(1, '2024-11-01 00:00:00', 'qiyan', 'futu', 11, '123456789', 'otpauth://totp/issuer?secret=ABCDEFG&algorithm=SHA1&digits=6&period=300', 1, '{\"ai_api_key\": \"{ai_api_key}\", \"ai_base_url\": \"https://dashscope.aliyuncs.com/compatible-mode/v1\", \"ai_api_model\": \"deepseek-r1\", \"longport_app_key\": \"{longport_app_key}\", \"ai_api_temperature\": \"1.0\", \"longport_app_secret\": \"{longport_app_secret}\", \"longport_access_token\": \"{longport_access_token}\"}');

-- --------------------------------------------------------

--
-- 表的结构 `owner_chat_record`
--

CREATE TABLE IF NOT EXISTS `owner_chat_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所有者',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息ID',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型（user: 用户消息, assistant: 助手消息）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `reasoning_content` text COLLATE utf8mb4_general_ci COMMENT 'AI思考内容',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话标题（股票+策略）',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_owner_session` (`owner`,`session_id`),
  KEY `idx_session_id` (`session_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=1898088647515967490 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI助手沟通记录表';

-- --------------------------------------------------------

--
-- 表的结构 `owner_order`
--

CREATE TABLE IF NOT EXISTS `owner_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `underlying_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `trade_from` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `trade_time` datetime NOT NULL,
  `strike_time` date DEFAULT NULL,
  `side` int NOT NULL,
  `price` decimal(10,4) NOT NULL,
  `order_fee` decimal(10,4) DEFAULT NULL,
  `quantity` int NOT NULL,
  `sub_order` int NOT NULL DEFAULT '0',
  `status` int NOT NULL,
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `platform_order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform_order_id_ex` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform_fill_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ext` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_o_o_f` (`owner`,`platform_order_id`,`platform_fill_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1898086399083810819 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 表的结构 `owner_security`
--

CREATE TABLE IF NOT EXISTS `owner_security` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id主键',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '证券名字',
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '证券代码',
  `market` int NOT NULL COMMENT '市场',
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '拥有者',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 1有效 0无效',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户证券表';

--
-- 转存表中的数据 `owner_security`
--

INSERT INTO `owner_security` (`id`, `create_time`, `name`, `code`, `market`, `owner`, `status`) VALUES
(1, '2025-02-16 14:08:13', '阿里巴巴', 'BABA', 11, 'qiyan', 1),
(2, '2025-02-16 14:08:13', '中概互联网', 'KWEB', 11, 'qiyan', 1),
(3, '2025-02-16 14:08:13', '京东', 'JD', 11, 'qiyan', 1),
(4, '2025-02-16 14:08:13', '中国大盘股', 'FXI', 11, 'qiyan', 0),
(5, '2025-02-28 22:53:12', '英伟达', 'NVDA', 11, 'qiyan', 1);

-- --------------------------------------------------------

--
-- 表的结构 `owner_strategy`
--

CREATE TABLE IF NOT EXISTS `owner_strategy` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略唯一编码',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `strategy_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略展示名字',
  `strategy_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略编码：如wheel_strategy',
  `stage` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略阶段',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 1有效 0无效',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '底层资产编码',
  `lot_size` int NOT NULL DEFAULT '100' COMMENT '每份合约股票数',
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '拥有者',
  `ext` json NOT NULL COMMENT '扩展信息',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 转存表中的数据 `owner_strategy`
--

INSERT INTO `owner_strategy` (`id`, `strategy_id`, `start_time`, `strategy_name`, `strategy_code`, `stage`, `status`, `code`, `lot_size`, `owner`, `ext`) VALUES
(18, '86c312eccaff4fe1865cf0e79432ebe3', '2024-11-01 00:00:00', '富途-BABA-默认策略', 'default', 'suspend', 1, 'BABA', 100, 'qiyan', '{}'),
(19, '81bab49eac5d427b967e93e5e93c9c68', '2025-02-14 00:00:00', '富途-BABA-车轮2张', 'wheel_strategy', 'running', 1, 'BABA', 100, 'qiyan', '{}'),
(20, 'aa9bcecb47ea454a9e38074f9efcd689', '2025-02-14 00:00:00', '富途-BABA-车轮1张', 'wheel_strategy', 'running', 1, 'BABA', 100, 'qiyan', '{}'),
(28, '024cc086a560460e90705f2ef87cac7d', '2024-11-01 00:00:00', '富途-KWEB-默认策略', 'default', 'suspend', 1, 'KWEB', 100, 'qiyan', '{}'),
(29, 'dd75cea90aff41f38dab4968c29b26d0', '2025-02-14 00:00:00', '富途-KWEB-车轮1张', 'wheel_strategy', 'running', 1, 'KWEB', 100, 'qiyan', '{}'),
(30, 'deff3bf54b7344a5b0d73cd54c7119d4', '2025-02-21 00:00:00', '富途-KWEB-车轮2张', 'wheel_strategy', 'running', 1, 'KWEB', 100, 'qiyan', '{}'),
(38, 'b3605b43f26345abbfa663abad867d38', '2024-11-01 00:00:00', '富途-JD-车轮策略', 'wheel_strategy', 'running', 1, 'JD', 100, 'qiyan', '{}'),
(58, '90eb0922828c4df6adb7e7108cb7c098', '2025-02-28 00:00:00', '富途-NVDA-车轮策略', 'wheel_strategy', 'running', 1, 'NVDA', 100, 'qiyan', '{}');
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
