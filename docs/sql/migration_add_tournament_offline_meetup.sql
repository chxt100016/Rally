-- 为已有赛事表增加线下赛活动关联。
ALTER TABLE `rally_tournament`
    ADD COLUMN `offline_meetup_id` VARCHAR(32) DEFAULT NULL COMMENT '线下赛活动的约球bizId' AFTER `offline_from_round`;
