-- Adminer 4.8.1 MySQL 8.2.0 dump

SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS `dev`;
CREATE DATABASE `dev` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `dev`;

DROP TABLE IF EXISTS `owner_account`;
CREATE TABLE `owner_account` (
  `id` int NOT NULL AUTO_INCREMENT,
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `platform` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `owner_order`;
CREATE TABLE `owner_order` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `trade_time` date NOT NULL,
  `side` int NOT NULL,
  `price` decimal(10,4) NOT NULL,
  `quantity` int NOT NULL,
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` int NOT NULL,
  `order_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `trade_market` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `ext` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `owner_security`;
CREATE TABLE `owner_security` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `ext` text COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

TRUNCATE `owner_security`;
INSERT INTO `owner_security` (`id`, `code`, `market`, `owner`, `ext`) VALUES
(1,	'BABA',	11,	'qiyan',	''),
(2,	'KWEB',	11,	'qiyan',	''),
(3,	'FXI',	11,	'qiyan',	''),
(4,	'JD',	11,	'qiyan',	'');

-- 2024-12-13 17:59:55