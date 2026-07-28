-- 约球卡片底图改为按枚举动态生成：
-- 1. rally_court 新增 surface（场地材质），删除 background_image
-- 2. rally_meetup 删除 background_image（不再落库，查询时按 court + 时段 + 天气动态算 key）

ALTER TABLE `rally_court`
  ADD COLUMN `surface` VARCHAR(32) DEFAULT NULL COMMENT '场地材质：HARD 硬地 / CLAY 红土 / GRASS 草地' AFTER `type`;

ALTER TABLE `rally_court`
  DROP COLUMN `background_image`;

ALTER TABLE `rally_meetup`
  DROP COLUMN `background_image`;
