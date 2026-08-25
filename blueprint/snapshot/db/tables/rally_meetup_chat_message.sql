CREATE TABLE `rally_meetup_chat_message` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`          VARCHAR(32)  NOT NULL COMMENT '消息业务主键（雪花ID）',
  `ref_id`          VARCHAR(32)  NOT NULL COMMENT '关联业务ID（约球ID或赛事ID）',
  `sender_id`       VARCHAR(32)  NOT NULL COMMENT '发送者 user_id',
  `sender_name`     VARCHAR(64)  NOT NULL DEFAULT '' COMMENT '发送者昵称（冗余存储）',
  `sender_avatar`   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '发送者头像URL（冗余存储）',
  `content`         TEXT NOT NULL COMMENT '消息内容（文本/图片URL/表情标识）',
  `content_type`    VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT-文本/IMAGE-图片/LOCATION-位置',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_ref_biz` (`ref_id`, `biz_id`) COMMENT '按关联业务+bizId游标拉取消息'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务聊天消息表';
