-- phpMyAdmin SQL Dump
-- version 5.2.2
-- https://www.phpmyadmin.net/
--
-- 主机： 10.0.12.220
-- 生成日期： 2025-08-10 03:45:05
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
-- 表的结构 `earnings_calendar`
--

CREATE TABLE `earnings_calendar` (
  `id` bigint NOT NULL COMMENT '主键',
  `symbol` varchar(20) NOT NULL COMMENT '股票代码',
  `name` varchar(255) NOT NULL COMMENT '公司名称',
  `market_cap` decimal(20,2) DEFAULT NULL COMMENT '市值',
  `fiscal_quarter_ending` varchar(20) DEFAULT NULL COMMENT '财报季度结束日期',
  `eps_forecast` decimal(10,2) DEFAULT NULL COMMENT '预期每股收益',
  `no_of_ests` int DEFAULT NULL COMMENT '分析师数量',
  `last_year_rpt_dt` varchar(20) DEFAULT NULL COMMENT '去年财报发布日期',
  `last_year_eps` decimal(10,2) DEFAULT NULL COMMENT '去年每股收益',
  `time` varchar(50) DEFAULT NULL COMMENT '财报发布时间',
  `earnings_date` date NOT NULL COMMENT '财报日期',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='财报日历表';

-- --------------------------------------------------------

--
-- 表的结构 `owner_account`
--

CREATE TABLE `owner_account` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '唯一用户名',
  `platform` varchar(16) COLLATE utf8mb4_general_ci NOT NULL COMMENT '平台',
  `market` int NOT NULL COMMENT '市场',
  `account_id` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
  `otp_auth` varchar(256) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'otp认证信息',
  `status` int NOT NULL COMMENT '状态 1有效 0无效',
  `ext` json DEFAULT NULL COMMENT '扩展配置，存储额外的配置信息，只支持一层配置，且只支持字符串类型'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账号表';

--
-- 转存表中的数据 `owner_account`
--

INSERT INTO `owner_account` (`id`, `create_time`, `owner`, `platform`, `market`, `account_id`, `otp_auth`, `status`, `ext`) VALUES
(1, '2024-11-01 00:00:00', 'qiyan', 'futu', 11, '123456789', 'otpauth://totp/issuer?secret=ABCDEFG&algorithm=SHA1&digits=6&period=300', 1, '{\"ai_api_key\": \"{ai_api_key}\", \"ai_base_url\": \"https://dashscope.aliyuncs.com/compatible-mode/v1\", \"ai_api_model\": \"deepseek-r1\", \"longport_app_key\": \"{longport_app_key}\", \"ai_api_temperature\": \"1.0\", \"longport_app_secret\": \"{longport_app_secret}\", \"longport_access_token\": \"{longport_access_token}\"}');

-- --------------------------------------------------------

--
-- 表的结构 `owner_chat_record`
--

CREATE TABLE `owner_chat_record` (
  `id` bigint NOT NULL COMMENT '记录ID',
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '所有者',
  `session_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID',
  `message_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '消息ID',
  `role` varchar(16) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息类型（user: 用户消息, assistant: 助手消息）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '消息内容',
  `reasoning_content` text COLLATE utf8mb4_general_ci COMMENT 'AI思考内容',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '会话标题（股票+策略）',
  `create_time` datetime NOT NULL COMMENT '创建时间',
  `update_time` datetime NOT NULL COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI助手沟通记录表';

-- --------------------------------------------------------

--
-- 表的结构 `owner_knowledge`
--

CREATE TABLE `owner_knowledge` (
  `id` bigint NOT NULL COMMENT '主键',
  `owner` varchar(50) NOT NULL COMMENT '所有者',
  `title` varchar(255) NOT NULL COMMENT '标题',
  `type` varchar(50) NOT NULL COMMENT '类型（OPTION_STRATEGY: 期权策略知识, RULE: 规则知识）',
  `description` varchar(1000) DEFAULT NULL COMMENT '描述',
  `content` text COMMENT '内容（支持Markdown）',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态（1: 启用, 0: 禁用）',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='所有者知识库表';

--
-- 转存表中的数据 `owner_knowledge`
--

INSERT INTO `owner_knowledge` (`id`, `owner`, `title`, `type`, `description`, `content`, `status`, `create_time`, `update_time`) VALUES
(1954185284060368897, 'qiyan', '车轮策略 (Wheel Strategy)', '1', '期权策略策略 Wheel Strategy', '### 第 1 阶段：确认股票趋势\n    * 必须向上趋势，否则暂停交易\n\n### 第 2 阶段：启动新策略之前进行预检查\n1. 市场安全评估  \n    ➠ 检查VIX恐慌指数  \n    • VIX＞30：停止操作（市场波动剧烈）  \n    • VIX≤30：进入下一检查\n    \n2. 个股交易安全性  \n    ➠ 检查期权到期前是否有财报发布  \n    • 有财报：规避（可能引发价格剧烈波动）  \n    • 无财报：继续评估\n    \n3. 技术指标分析（基于周线图）  \n    ➠ 检查相对强弱指数RSI  \n    • RSI＜30（超卖区域）：进行MACD验证  \n    • RSI≥30：直接进入第四项检查\n    \n    ➠ MACD指标验证（参数12,26,9）  \n    • 动量向上：表明股价可能从支撑位反弹，进入最终检查  \n    • 动量向下：规避该股（存在强烈下跌趋势）\n    \n4. 关键支撑位判定  \n    ➠ 观察1年周期周线图  \n    • 接近支撑位：纳入候选名单  \n    • 远离支撑位：暂时暂停交易（价格走势不可预测）\n    \n（注：主要交易周合约而非月合约，因此技术分析基于周线级别）\n\n### 第 3 阶段：选择股票以启动新策略\n* **年化回报率> 30%**（（期权合约价格 x 100）/（DTE） * 一年 365 天/除以股价 x 100）\n* 按回报潜力对剩余股票进行排名，并选择**回报率最高的**合约\n\n### 第 4 阶段：管理 wheel\n* SELL PUT\n  * **何时平仓/展期**：合约在到期前达到 **80% 的盈利能力** ，平仓并开始新的策略（遵循第 3 阶段概述的相同步骤）\n  * **何时接受转让**：如果股票价格等于或低于行使价，接受转让。由于这些是乐于持有的股票，因此接下来将继续出售备兑看涨期权。\n* SELL CALL\n  * **将执行价格设置为等于或高于购买价格**：为避免亏本出售，确保执行价格**至少**是购买股票的价格。\n  * 如果股票跌破这个价格，有两个选择：\n    * 等待\n    * 通过卖出另一份看跌合约来平均下跌（谨慎，遵循我在第 3 阶段概述的执行价格规则）。\n  * **何时接受指派**：**总是** (会错过潜在的收益，但轮盘策略带来稳定现金流)\n\n### 附加信息\n* 车轮策略中的看跌期权：关于 delta，通常保持在 .25 - .35 的范围内;\n* 车轮策略中的看涨期权：不关注看涨期权的 delta - 通常每周卖出至少我购买股票的价格高 1 美元的看涨期权;\n', 1, '2025-08-09 22:17:33', '2025-08-09 22:29:00'),
(1954185532824539137, 'qiyan', '备兑看涨策略 (Covered Call)', '1', '备兑看涨策略 Covered Call', '### 一、开仓规则\n➦ **初始开仓**：优先平值期权（ATM）\n➦ **到期日选择**：优先选择月度期权\n\n### 二、Delta监控（策略整体Delta=股票Delta+期权Delta）\n- **目标区间**：0.25 ≤ Delta ≤ 0.75 \n- **超出区间** → 触发调整\n\n### 三、调整规则\n（注：策略整体Delta不在0.25到0.75之间时，才触发调整）\n1. 短周期（到期前 2-3周内）\n  ➠ 首选操作：\n  • 滚动至下月平价期权（ATM）。\n\n  ➠ 次选操作（按Delta动态调整）：\n  • 当 Delta > 0.5：降低 Delta 0.1（例如 0.6 → 0.5）。\n  • 当 Delta < 0.5：提高 Delta 0.1（例如 0.4 → 0.5）。  \n    \n2. 中周期（到期时间 3个月内）  \n  ➠ 通用规则：\n  • 无论Delta数值如何，强制滚动至下一到期月的合约。\n\n  ➠ 特殊匹配规则：\n  • 若当前 Delta = 0.75 → 选择下月合约 Delta = 0.50\n  • 目标Delta：0.50\n  • 若当前 Delta = 0.25 → 选择下月合约 Delta = 0.40\n  • 目标Delta：0.40\n    \n3. 长周期（到期时间 > 3个月）  \n  ➠ 条件：Delta ≥ 0.75\n  • 操作：滚动至同到期日的期权合约\n  • 目标Delta：0.50\n  ➠ 条件：Delta ≤ 0.25\n  • 操作：滚动至同到期日的期权合约\n  • 目标Delta：0.40', 1, '2025-08-09 22:18:33', '2025-08-09 22:29:10'),
(1954237135095029762, 'qiyan', '交易偏好', '2', '交易偏好规则', '1. 优先使用偏保守的策略。\n2. 期权可能被行权时，优先选择Roll。', 1, '2025-08-10 01:43:36', '2025-08-10 01:43:36');

-- --------------------------------------------------------

--
-- 表的结构 `owner_order`
--

CREATE TABLE `owner_order` (
  `id` bigint NOT NULL,
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
  `ext` json DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- 表的结构 `owner_security`
--

CREATE TABLE `owner_security` (
  `id` bigint NOT NULL COMMENT 'id主键',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `name` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '证券名字',
  `code` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '证券代码',
  `market` int NOT NULL COMMENT '市场',
  `owner` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '拥有者',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 1有效 0无效'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户证券表';

--
-- 转存表中的数据 `owner_security`
--

INSERT INTO `owner_security` (`id`, `create_time`, `name`, `code`, `market`, `owner`, `status`) VALUES
(1, '2025-02-16 14:08:13', '阿里巴巴', 'BABA', 11, 'qiyan', 1),
(2, '2025-02-16 14:08:13', '中概互联网', 'KWEB', 11, 'qiyan', 1),
(3, '2025-02-16 14:08:13', '京东', 'JD', 11, 'qiyan', 1),
(4, '2025-02-16 14:08:13', '中国大盘股', 'FXI', 11, 'qiyan', 0),
(5, '2025-02-28 22:53:12', '英伟达', 'NVDA', 11, 'qiyan', 1),
(6, '2025-03-11 00:47:31', '特斯拉', 'TSLA', 11, 'qiyan', 1);

-- --------------------------------------------------------

--
-- 表的结构 `owner_strategy`
--

CREATE TABLE `owner_strategy` (
  `id` bigint NOT NULL COMMENT '主键ID',
  `strategy_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略唯一编码',
  `start_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '开始时间',
  `strategy_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略展示名字',
  `strategy_code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略编码：如wheel_strategy',
  `stage` varchar(32) COLLATE utf8mb4_general_ci NOT NULL COMMENT '策略阶段',
  `status` int NOT NULL DEFAULT '1' COMMENT '状态 1有效 0无效',
  `code` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '底层资产编码',
  `lot_size` int NOT NULL DEFAULT '100' COMMENT '每份合约股票数',
  `owner` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '拥有者',
  `ext` json NOT NULL COMMENT '扩展信息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- 转存表中的数据 `owner_strategy`
--

INSERT INTO `owner_strategy` (`id`, `strategy_id`, `start_time`, `strategy_name`, `strategy_code`, `stage`, `status`, `code`, `lot_size`, `owner`, `ext`) VALUES
(18, '86c312eccaff4fe1865cf0e79432ebe3', '2024-11-01 00:00:00', 'BABA-默认策略', 'default', 'running', 1, 'BABA', 100, 'qiyan', '{\"initial_stock_num\": \"100\"}'),
(19, '81bab49eac5d427b967e93e5e93c9c68', '2025-02-14 00:00:00', 'BABA-车轮2张', 'wheel_strategy', 'running', 1, 'BABA', 100, 'qiyan', '{\"initial_stock_num\": \"200\", \"initial_stock_cost\": \"120\", \"wheel_sellput_strike_price\": \"120\"}'),
(20, 'aa9bcecb47ea454a9e38074f9efcd689', '2025-02-14 00:00:00', 'BABA-车轮1张', 'wheel_strategy', 'running', 1, 'BABA', 100, 'qiyan', '{\"initial_stock_num\": \"100\", \"initial_stock_cost\": \"115\", \"wheel_sellput_strike_price\": \"115\"}'),
(21, 'aa6406b0f2c04483b75607b2824d14e3', '2025-04-17 23:28:34', 'BABA-长期CC', 'cc_strategy', 'running', 1, 'BABA', 100, 'qiyan', '{\"initial_stock_num\": \"200\"}'),
(22, '5609682f617d41ab873bb8d5a2b0a5c1', '2025-05-27 23:01:36', 'BABA-长期CC-v2', 'cc_strategy', 'running', 1, 'BABA', 100, 'qiyan', '{\"initial_stock_num\": \"200\"}'),
(28, '024cc086a560460e90705f2ef87cac7d', '2024-11-01 00:00:00', 'KWEB-默认策略', 'default', 'running', 1, 'KWEB', 100, 'qiyan', '{\"initial_stock_num\": \"0\"}'),
(29, 'dd75cea90aff41f38dab4968c29b26d0', '2025-02-14 00:00:00', 'KWEB-车轮1张', 'wheel_strategy', 'running', 1, 'KWEB', 100, 'qiyan', '{\"initial_stock_num\": \"100\", \"initial_stock_cost\": \"34\", \"wheel_sellput_strike_price\": \"34\"}'),
(30, 'deff3bf54b7344a5b0d73cd54c7119d4', '2025-02-21 00:00:00', 'KWEB-车轮2张', 'wheel_strategy', 'running', 1, 'KWEB', 100, 'qiyan', '{\"initial_stock_num\": \"200\", \"initial_stock_cost\": \"35\", \"wheel_sellput_strike_price\": \"35\"}'),
(38, 'b3605b43f26345abbfa663abad867d38', '2024-11-01 00:00:00', 'JD-车轮策略', 'wheel_strategy', 'running', 1, 'JD', 100, 'qiyan', '{\"initial_stock_num\": \"0\"}'),
(58, '90eb0922828c4df6adb7e7108cb7c098', '2025-02-28 00:00:00', 'NVDA-车轮策略', 'wheel_strategy', 'running', 1, 'NVDA', 100, 'qiyan', '{\"initial_stock_num\": \"0\", \"wheel_sellput_strike_price\": \"100\"}');

--
-- 转储表的索引
--

--
-- 表的索引 `earnings_calendar`
--
ALTER TABLE `earnings_calendar`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_symbol_earnings_date` (`symbol`,`earnings_date`),
  ADD KEY `idx_earnings_date` (`earnings_date`),
  ADD KEY `idx_symbol` (`symbol`);

--
-- 表的索引 `owner_account`
--
ALTER TABLE `owner_account`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_owner` (`owner`),
  ADD UNIQUE KEY `uk_platform_market_account` (`platform`,`account_id`,`market`) USING BTREE;

--
-- 表的索引 `owner_chat_record`
--
ALTER TABLE `owner_chat_record`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_owner_session` (`owner`,`session_id`),
  ADD KEY `idx_session_id` (`session_id`),
  ADD KEY `idx_create_time` (`create_time`);

--
-- 表的索引 `owner_knowledge`
--
ALTER TABLE `owner_knowledge`
  ADD PRIMARY KEY (`id`),
  ADD KEY `idx_owner` (`owner`),
  ADD KEY `idx_type` (`type`),
  ADD KEY `idx_status` (`status`);

--
-- 表的索引 `owner_order`
--
ALTER TABLE `owner_order`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `uk_o_o_f` (`owner`,`platform_order_id`,`platform_fill_id`) USING BTREE;

--
-- 表的索引 `owner_security`
--
ALTER TABLE `owner_security`
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
-- 使用表AUTO_INCREMENT `earnings_calendar`
--
ALTER TABLE `earnings_calendar`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键';

--
-- 使用表AUTO_INCREMENT `owner_account`
--
ALTER TABLE `owner_account`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', AUTO_INCREMENT=2;

--
-- 使用表AUTO_INCREMENT `owner_chat_record`
--
ALTER TABLE `owner_chat_record`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID';

--
-- 使用表AUTO_INCREMENT `owner_knowledge`
--
ALTER TABLE `owner_knowledge`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键', AUTO_INCREMENT=10000;

--
-- 使用表AUTO_INCREMENT `owner_order`
--
ALTER TABLE `owner_order`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- 使用表AUTO_INCREMENT `owner_security`
--
ALTER TABLE `owner_security`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'id主键', AUTO_INCREMENT=7;

--
-- 使用表AUTO_INCREMENT `owner_strategy`
--
ALTER TABLE `owner_strategy`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID', AUTO_INCREMENT=10000;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
