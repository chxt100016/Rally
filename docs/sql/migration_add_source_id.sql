-- 为 rally_court 表添加 source_id 字段
-- 执行时间: 2026-07-02

ALTER TABLE `rally_court`
ADD COLUMN `source_id` VARCHAR(128) DEFAULT NULL COMMENT '三方来源ID，用于标识球场在第三方系统中的唯一ID' AFTER `source`;

-- 添加唯一索引
ALTER TABLE `rally_court`
ADD UNIQUE KEY `uk_source_id` (`source_id`);
