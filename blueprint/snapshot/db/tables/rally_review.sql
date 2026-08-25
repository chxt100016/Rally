CREATE TABLE `rally_review` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `from_user_id`    VARCHAR(32) NOT NULL COMMENT '评价人 user_id（仅本人，不可代评）',
  `to_user_id`      VARCHAR(32) NOT NULL COMMENT '被评价人 user_id',
  `review_type`     VARCHAR(16) NOT NULL COMMENT '评价维度：ntrp_vote 水平三元投票 / attendance 出勤 / tag 个性化标签',
  `review_value`    VARCHAR(512) NOT NULL COMMENT '评价值。ntrp_vote: higher/same/lower；attendance: on_time/late/no_show；tag: 逗号分隔的多标签',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  -- 每场每人对另一人的每个维度只有一行：存在则更新（tag 整串逗号分隔），不删旧
  UNIQUE KEY `uk_review_dim` (`rally_meetup_id`,`from_user_id`,`to_user_id`,`review_type`),
  KEY `idx_to_user` (`to_user_id`,`review_type`) COMMENT '球员主页按被评价人+维度聚合',
  KEY `idx_meetup_from` (`rally_meetup_id`,`from_user_id`) COMMENT '查我在某场已评状态'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评价竖表（NTRP三元投票/出勤/个性化标签，每维度一行，tag 逗号分隔）';
