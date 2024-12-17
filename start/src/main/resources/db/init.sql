-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- 主机： 10.0.12.220
-- 生成日期： 2024-12-17 17:50:34
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

-- --------------------------------------------------------

--
-- 表的结构 `owner_order`
--

CREATE TABLE `owner_order` (
  `id` int NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `underlying_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `trade_time` date NOT NULL,
  `strike_time` date DEFAULT NULL,
  `side` int NOT NULL,
  `price` decimal(10,4) NOT NULL,
  `quantity` int NOT NULL,
  `status` int NOT NULL,
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `platform_order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `platform` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ext` json DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- --------------------------------------------------------

--
-- 表的结构 `owner_strategy`
--

CREATE TABLE `owner_strategy` (
  `id` int NOT NULL,
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `strategy_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `strategy_name` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `current_stage` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `platform` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `ext` json NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 转存表中的数据 `owner_strategy`
--

INSERT INTO `owner_strategy` (`id`, `strategy_id`, `strategy_type`, `strategy_name`, `current_stage`, `platform`, `account_id`, `code`, `market`, `owner`, `ext`) VALUES
(18, '86c312eccaff4fe1865cf0e79432ebe3', 'wheel_strategy', 'BABA-富途Wheel', 'sp', 'futu', '123456', 'BABA', 11, 'qiyan', '{}'),
(19, '30132f0ae0fc4a07a49cae550a08cae9', 'wheel_strategy', 'BABA-富途CC', 'cc', 'futu', '123456', 'BABA', 11, 'qiyan', '{}'),
(28, '024cc086a560460e90705f2ef87cac7d', 'wheel_strategy', 'KWEB-富途CC', 'cc', 'futu', '123456', 'KWEB', 11, 'qiyan', '{}'),
(38, 'b3605b43f26345abbfa663abad867d38', 'wheel_strategy', 'JD-富途Wheel', 'sp', 'futu', '123456', 'JD', 11, 'qiyan', '{}'),
(58, '8a533bd8ce2e41bf93a4dec6347fdf49', 'wheel_strategy', 'FXI-长桥Wheel', 'cc', 'longport', '', 'FXI', 11, 'qiyan', '{}');

--
-- 转储表的索引
--

--
-- 表的索引 `owner_order`
--
ALTER TABLE `owner_order`
  ADD PRIMARY KEY (`id`);

--
-- 表的索引 `owner_strategy`
--
ALTER TABLE `owner_strategy`
  ADD PRIMARY KEY (`id`);

--
-- 在导出的表使用AUTO_INCREMENT
--

--
-- 使用表AUTO_INCREMENT `owner_order`
--
ALTER TABLE `owner_order`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=1003;

--
-- 使用表AUTO_INCREMENT `owner_strategy`
--
ALTER TABLE `owner_strategy`
  MODIFY `id` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=59;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
