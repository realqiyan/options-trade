-- 添加code字段到owner_knowledge表
ALTER TABLE owner_knowledge ADD COLUMN code VARCHAR(255) COMMENT '编码';

-- 添加唯一索引
ALTER TABLE owner_knowledge ADD UNIQUE INDEX uk_owner_code (owner, code);