-- 为 rally_court、rally_meetup 表添加 background_image 字段
-- 执行时间: 2026-07-26

ALTER TABLE `rally_court`
ADD COLUMN `background_image` VARCHAR(256) DEFAULT NULL COMMENT '背景图（七牛云 key）' AFTER `meetup_count`;

ALTER TABLE `rally_meetup`
ADD COLUMN `background_image` VARCHAR(256) DEFAULT NULL COMMENT '背景图（七牛云 key），TEXT/MAP模式下取自球场库' AFTER `court_id`;
