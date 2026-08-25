CREATE TABLE `user_follow` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`        VARCHAR(32)  NOT NULL COMMENT '雪花 ID（业务主键）',
  `follower_id`   VARCHAR(32)  NOT NULL COMMENT '关注人 user_id',
  `following_id`  VARCHAR(32)  NOT NULL COMMENT '被关注人 user_id',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_follow_rel` (`follower_id`, `following_id`) COMMENT '同一关注关系唯一',
  KEY `idx_following` (`following_id`) COMMENT '查被关注（粉丝）列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户关注关系表';
