-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主机： 10.0.12.220
-- 生成日期： 2024-12-21 17:12:04
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
-- 表的结构 `owner_order`
--

DROP TABLE IF EXISTS `owner_order`;
CREATE TABLE IF NOT EXISTS `owner_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `underlying_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `trade_from` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `trade_time` datetime NOT NULL,
  `strike_time` date DEFAULT NULL,
  `side` int NOT NULL,
  `price` decimal(10,4) NOT NULL,
  `quantity` int NOT NULL,
  `status` int NOT NULL,
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `platform_order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform_order_id_ex` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform_fill_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ext` json DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `pk_p_o_f` (`platform`,`platform_order_id`,`platform_fill_id`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=1663963139 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 插入之前先把表清空（truncate） `owner_order`
--

TRUNCATE TABLE `owner_order`;

-- --------------------------------------------------------

--
-- 表的结构 `owner_strategy`
--

DROP TABLE IF EXISTS `owner_strategy`;
CREATE TABLE IF NOT EXISTS `owner_strategy` (
  `id` int NOT NULL AUTO_INCREMENT,
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `strategy_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` int NOT NULL DEFAULT '1',
  `platform` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `lot_size` int NOT NULL DEFAULT '100',
  `market` int NOT NULL,
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `ext` json NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_p_a_c` (`platform`,`account_id`,`code`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=100 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 插入之前先把表清空（truncate） `owner_strategy`
--

TRUNCATE TABLE `owner_strategy`;
--
-- 转存表中的数据 `owner_strategy`
--

INSERT INTO `owner_strategy` (`id`, `strategy_id`, `start_time`, `strategy_name`, `status`, `platform`, `account_id`, `code`, `lot_size`, `market`, `owner`, `ext`) VALUES
(18, '86c312eccaff4fe1865cf0e79432ebe3', '2024-11-01 00:00:00', 'BABA-富途', 1, 'futu', '123456', 'BABA', 100, 11, 'qiyan', '{}'),
(28, '024cc086a560460e90705f2ef87cac7d', '2024-11-01 00:00:00', 'KWEB-富途', 1, 'futu', '123456', 'KWEB', 100, 11, 'qiyan', '{}'),
(38, 'b3605b43f26345abbfa663abad867d38', '2024-11-01 00:00:00', 'JD-富途', 1, 'futu', '123456', 'JD', 100, 11, 'qiyan', '{}');
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
