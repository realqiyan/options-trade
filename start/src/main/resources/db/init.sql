-- Adminer 4.8.1 MySQL 8.2.0 dump

SET NAMES utf8;
SET time_zone = '+00:00';
SET foreign_key_checks = 0;
SET sql_mode = 'NO_AUTO_VALUE_ON_ZERO';

SET NAMES utf8mb4;

DROP DATABASE IF EXISTS `dev`;
CREATE DATABASE `dev` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `dev`;

DROP TABLE IF EXISTS `owner_order`;
CREATE TABLE `owner_order` (
  `id` int NOT NULL AUTO_INCREMENT,
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
  `ext` json DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


DROP TABLE IF EXISTS `owner_strategy`;
CREATE TABLE `owner_strategy` (
  `id` int NOT NULL AUTO_INCREMENT,
  `strategy_id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `strategy_type` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `platform` varchar(16) COLLATE utf8mb4_general_ci NOT NULL,
  `account_id` varchar(32) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `market` int NOT NULL,
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL,
  `ext` json NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


-- 2024-12-14 18:42:07