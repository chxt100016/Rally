CREATE TABLE `payment_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`         VARCHAR(32)  NOT NULL COMMENT '雪花ID',
  `channel`        VARCHAR(16)  NOT NULL DEFAULT 'WECHAT' COMMENT '支付渠道',
  `log_type`       VARCHAR(24)  NOT NULL COMMENT '日志类型：COLLECT 发起收款建单 / PREPAY 参与人发起支付下单 / CALLBACK 渠道回调',
  `ref_type`       VARCHAR(16)  DEFAULT NULL COMMENT '关联单类型：ORDER 支付单',
  `ref_id`         VARCHAR(64)  DEFAULT NULL COMMENT '关联单号（payment_order biz_id，即渠道 out_trade_no）',
  `raw_body`       TEXT         DEFAULT NULL COMMENT '原始报文：下单请求/响应、回调解密后报文，对账留痕',
  `process_status` VARCHAR(16)  NOT NULL DEFAULT 'PROCESSED' COMMENT '处理状态：RECEIVED/PROCESSED/FAILED。COLLECT/PREPAY 纯留痕，落库即 PROCESSED；仅 CALLBACK 落 RECEIVED 待处理，供补偿扫描',
  `remark`         VARCHAR(255) DEFAULT NULL COMMENT '处理备注/失败原因',
  `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_ref` (`ref_id`) COMMENT '按关联单号排查全链路',
  KEY `idx_type_status` (`log_type`, `process_status`) COMMENT '回调未处理补偿扫描'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付全链路留痕（建单/下单/回调，对账与排查）';
