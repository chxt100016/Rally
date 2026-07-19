-- ============================================================
-- 数据迁移：cost_items -> cost_data
-- ============================================================

-- 1. 添加新字段 cost_data
ALTER TABLE `rally_meetup` ADD COLUMN `cost_data` JSON DEFAULT NULL COMMENT '费用数据 {costItems:[{name,totalAmount(分)}], hourlyAllocations:[{duration,userIds}]}';

-- 2. 迁移历史数据（将 cost_items 数组包装成对象）
UPDATE `rally_meetup`
SET `cost_data` = JSON_OBJECT('costItems', cost_items, 'hourlyAllocations', NULL)
WHERE `cost_items` IS NOT NULL;

-- 3. 处理 cost_items 为 NULL 的情况
UPDATE `rally_meetup`
SET `cost_data` = NULL
WHERE `cost_items` IS NULL;

-- 4. 删除旧字段 cost_items
ALTER TABLE `rally_meetup` DROP COLUMN `cost_items`;
