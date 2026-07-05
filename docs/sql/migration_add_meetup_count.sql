-- 为 rally_court 表添加 meetup_count 字段
-- 用于统计每个球场的约球次数

ALTER TABLE `rally_court` ADD COLUMN `meetup_count` INT DEFAULT 0 COMMENT '约球次数统计';
