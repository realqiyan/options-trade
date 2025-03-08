-- 添加ext字段到owner_account表
ALTER TABLE `owner_account` ADD COLUMN `ext` json DEFAULT NULL COMMENT '扩展配置，存储额外的配置信息';

-- 更新现有数据，设置默认的ext值
UPDATE `owner_account` SET `ext` = '{}' WHERE `ext` IS NULL;

-- 创建索引
ALTER TABLE `owner_account` ADD INDEX `idx_owner_account_platform` (`platform`);
ALTER TABLE `owner_account` ADD INDEX `idx_owner_account_status` (`status`);

-- 添加注释
ALTER TABLE `owner_account` MODIFY COLUMN `ext` json DEFAULT NULL COMMENT '扩展配置，存储额外的配置信息，只支持一层配置，且只支持字符串类型';

-- 示例：将配置从properties文件迁移到数据库
-- 长桥平台配置示例
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.longport_app_key', '{LONGPORT_APP_KEY}') WHERE `platform` = 'longport';
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.longport_app_secret', '{LONGPORT_APP_SECRET}') WHERE `platform` = 'longport';
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.longport_access_token', '{LONGPORT_ACCESS_TOKEN}') WHERE `platform` = 'longport';

-- AI配置示例
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.ai_base_url', 'https://dashscope.aliyuncs.com/compatible-mode/v1');
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.ai_api_model', 'deepseek-r1');
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.ai_api_key', 'sk-00000000000000000000000000000000');
-- UPDATE `owner_account` SET `ext` = JSON_SET(`ext`, '$.ai_api_temperature', '1.0'); 