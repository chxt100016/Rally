CREATE TABLE `user_notify_subscribe` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键(内部用,CAS/回写定位)',
  `biz_id`       VARCHAR(32)  NOT NULL COMMENT '雪花流水ID(流程ID)',
  `user_id`      VARCHAR(32)  NOT NULL COMMENT '接收人(被通知用户)',
  `biz_type`     VARCHAR(32)  NOT NULL COMMENT '业务方向:MEETUP(后续扩展)',
  `ref_biz_id`   VARCHAR(32)  NOT NULL COMMENT '关联业务对象ID(当前=meetupId)',
  `notice_scene` VARCHAR(32)  NOT NULL COMMENT '场景:JOIN_SUCCESS/PENDING_APPROVAL/MEETUP_CANCEL/TEAM_SUCCESS',
  `template_id`  VARCHAR(64)  NOT NULL COMMENT '微信订阅模板ID(冗余,便于排查)',
  `status`       VARCHAR(16)  NOT NULL DEFAULT 'UNUSED' COMMENT '状态:UNUSED/SENDING/SENT/FAILED/EXPIRED',
  `fail_reason`  VARCHAR(255) DEFAULT NULL COMMENT '失败原因(微信errcode/errmsg)',
  `send_time`    DATETIME     DEFAULT NULL COMMENT '发送时间',
  `expire_time`  DATETIME     DEFAULT NULL COMMENT '订阅额度过期时间',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_ref_scene_status`  (`biz_type`, `ref_biz_id`, `notice_scene`, `status`),
  KEY `idx_user_scene_status` (`user_id`, `notice_scene`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅通知流水';
