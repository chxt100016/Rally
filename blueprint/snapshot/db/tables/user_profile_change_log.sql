CREATE TABLE `user_profile_change_log` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`       VARCHAR(32)  NOT NULL COMMENT '雪花 ID',
  `user_id`      VARCHAR(32)  NOT NULL COMMENT '用户',
  `type`         VARCHAR(16) NOT NULL COMMENT '变更类型',
  `before_value` DECIMAL(6,2) DEFAULT NULL COMMENT '变更前值；under_review 进入时存锁定场数(required_matches)',
  `after_value`  DECIMAL(6,2) DEFAULT NULL COMMENT '变更后值；under_review 进度推进时存剩余场数',
  `value`        DECIMAL(6,2) DEFAULT NULL COMMENT '变更量（+向上/-向下）',
  `reason`       VARCHAR(32)  NOT NULL COMMENT '原因枚举：user 手动 / system 系统 / system_suggest 建议免核查 / review_bad 遇差票 等',
  `remark`       VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `ref_id`       VARCHAR(32)  DEFAULT NULL COMMENT '关联业务 biz_id（如 meetup_id / review_id），可空',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_idempotent` (`user_id`, `type`, `ref_id`, `reason`),
  KEY `idx_user_type` (`user_id`, `type`),
  KEY `idx_ref` (`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户档案变更日志';
