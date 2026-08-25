CREATE TABLE `rally_meetup_registration` (
  `id`              BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32) NOT NULL COMMENT '业务主键（雪花算法字符串）',
  `rally_meetup_id` VARCHAR(32) NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `user_id`         VARCHAR(32) NOT NULL COMMENT '参与人（含创建者），关联 users.user_id',
  `status`          varchar(16) NOT NULL DEFAULT 'pending' COMMENT '报名状态机：PENDING/JOINED/REJECTED/WITHDRAWN/REVIEWED/SKIPPED/QUIT',
  `expires_at`      DATETIME    DEFAULT NULL COMMENT '自动撤回失效时间，NULL=不自动撤回',
  `opt_time`        DATETIME    DEFAULT NULL COMMENT '管理人审批操作时间',
  `create_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  `update_time`     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_user_status` (`user_id`, `status`) COMMENT '查我的报名 + 冲突检测',
  KEY `idx_meetup_status` (`rally_meetup_id`, `status`) COMMENT '审批列表'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='约球报名/注册表（记录所有参与者：创建者、等待审批、已通过等）';
