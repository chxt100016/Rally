-- 记录报名用户最近一次打开赛事详情的时间。
ALTER TABLE `rally_tournament_entry`
    ADD COLUMN `last_visit_time` DATETIME DEFAULT NULL COMMENT '用户最近一次打开本赛事详情的时间' AFTER `paid_time`;
