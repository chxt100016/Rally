-- 将约球专用聊天关联字段泛化为业务关联ID。
-- 执行前请确认当前表结构仍使用 meetup_id 及下列索引名。

ALTER TABLE `rally_meetup_chat_message`
  DROP INDEX `idx_meetup_biz`,
  CHANGE COLUMN `meetup_id` `ref_id` VARCHAR(32) NOT NULL COMMENT '关联业务ID（约球ID或赛事ID）',
  ADD KEY `idx_ref_biz` (`ref_id`, `biz_id`) COMMENT '按关联业务+bizId游标拉取消息';

ALTER TABLE `rally_meetup_chat_user`
  DROP INDEX `uk_meetup_user`,
  DROP INDEX `idx_meetup`,
  CHANGE COLUMN `meetup_id` `ref_id` VARCHAR(32) NOT NULL COMMENT '关联业务ID（约球ID或赛事ID）',
  ADD UNIQUE KEY `uk_ref_user` (`ref_id`, `user_id`) COMMENT '每个用户在每个关联业务只有一条记录',
  ADD KEY `idx_ref` (`ref_id`) COMMENT '查询关联业务的所有参与者';
