-- 为 rally_meetup 表添加 court_select_mode 和 court_id 字段
-- 执行时间: 2026-07-03

ALTER TABLE `rally_meetup`
ADD COLUMN `court_select_mode` VARCHAR(8) DEFAULT NULL COMMENT '球场选择模式：TEXT/MAP/FREE' AFTER `court_lat`,
ADD COLUMN `court_id` VARCHAR(32) DEFAULT NULL COMMENT '球场库ID，TEXT/MAP模式下从球场库选中时传入' AFTER `court_select_mode`;
