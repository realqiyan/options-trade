-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主机： 10.0.12.220
-- 生成日期： 2025-02-16 17:08:59
-- 服务器版本： 8.2.0
-- PHP 版本： 8.2.26

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
CREATE DATABASE IF NOT EXISTS `options` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `options`;

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
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_owner` (`owner`),
  UNIQUE KEY `uk_platform_market_account` (`platform`,`account_id`,`market`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账号表';

--
-- 转存表中的数据 `owner_account`
--

INSERT INTO `owner_account` (`id`, `create_time`, `owner`, `platform`, `market`, `account_id`, `otp_auth`, `status`) VALUES
(1, '2024-11-01 00:00:00', 'qiyan', 'futu', 11, '123456789', 'otpauth://totp/issuer?secret=ABCDEFG&algorithm=SHA1&digits=6&period=300', 1);

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
) ENGINE=InnoDB AUTO_INCREMENT=2142642178 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

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
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户证券表';

--
-- 转存表中的数据 `owner_security`
--

INSERT INTO `owner_security` (`id`, `create_time`, `name`, `code`, `market`, `owner`, `status`) VALUES
(1, '2025-02-16 14:08:13', '阿里巴巴', 'BABA', 11, 'qiyan', 1),
(2, '2025-02-16 14:08:13', '中国海外互联网', 'KWEB', 11, 'qiyan', 1),
(3, '2025-02-16 14:08:13', '京东', 'JD', 11, 'qiyan', 1),
(4, '2025-02-16 14:08:13', '中国大盘股', 'FXI', 11, 'qiyan', 0);

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
(18, '86c312eccaff4fe1865cf0e79432ebe3', '2024-11-01 00:00:00', '富途-BABA-默认策略', 'default', 'default', 1, 'BABA', 100, 'qiyan', '{}'),
(19, '81bab49eac5d427b967e93e5e93c9c68', '2025-02-14 00:00:00', '富途-BABA-车轮2张', 'wheel_strategy', 'sellput', 1, 'BABA', 100, 'qiyan', '{}'),
(20, 'aa9bcecb47ea454a9e38074f9efcd689', '2025-02-14 00:00:00', '富途-BABA-车轮1张', 'wheel_strategy', 'sellput', 1, 'BABA', 100, 'qiyan', '{}'),
(28, '024cc086a560460e90705f2ef87cac7d', '2024-11-01 00:00:00', '富途-KWEB-默认策略', 'default', 'default', 1, 'KWEB', 100, 'qiyan', '{}'),
(38, 'b3605b43f26345abbfa663abad867d38', '2024-11-01 00:00:00', '富途-JD-默认策略', 'default', 'default', 1, 'JD', 100, 'qiyan', '{}');
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
