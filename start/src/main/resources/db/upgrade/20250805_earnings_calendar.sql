CREATE TABLE `earnings_calendar` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `symbol` varchar(20) NOT NULL COMMENT '股票代码',
  `name` varchar(255) NOT NULL COMMENT '公司名称',
  `market_cap` decimal(20,2) DEFAULT NULL COMMENT '市值',
  `fiscal_quarter_ending` varchar(20) DEFAULT NULL COMMENT '财报季度结束日期',
  `eps_forecast` decimal(10,2) DEFAULT NULL COMMENT '预期每股收益',
  `no_of_ests` int(11) DEFAULT NULL COMMENT '分析师数量',
  `last_year_rpt_dt` varchar(20) DEFAULT NULL COMMENT '去年财报发布日期',
  `last_year_eps` decimal(10,2) DEFAULT NULL COMMENT '去年每股收益',
  `time` varchar(50) DEFAULT NULL COMMENT '财报发布时间',
  `earnings_date` date NOT NULL COMMENT '财报日期',
  `created_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_symbol_earnings_date` (`symbol`,`earnings_date`),
  KEY `idx_earnings_date` (`earnings_date`),
  KEY `idx_symbol` (`symbol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='财报日历表';