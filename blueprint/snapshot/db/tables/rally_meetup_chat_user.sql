CREATE TABLE `rally_meetup_chat_user` (
  `id`                  BIGINT      NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`              VARCHAR(32) NOT NULL COMMENT '业务主键（雪花ID）',
  `ref_id`              VARCHAR(32) NOT NULL COMMENT '关联业务ID（约球ID或赛事ID）',
  `user_id`             VARCHAR(32) NOT NULL COMMENT '用户 user_id',
  `last_read_message_id` VARCHAR(32) NOT NULL DEFAULT '' COMMENT '已读最新消息bizId（仅用于未读数计算）',
  `last_read_time`      DATETIME    NULL COMMENT '最后一次已读时间（仅真实拉取触发已读时更新）',
  `unread_count`        INT         NOT NULL DEFAULT 0 COMMENT '未读消息数（冗余存储）',
  `joined_at`           DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入聊天时间',
  `create_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`         DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_ref_user` (`ref_id`, `user_id`) COMMENT '每个用户在每个关联业务只有一条记录',
  KEY `idx_ref` (`ref_id`) COMMENT '查询关联业务的所有参与者'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务聊天用户表（含已读状态）';
