-- Rally 支付域建表语句（MVP：赛事报名费）
-- 范围：报名成功后参与者微信支付报名费。
-- MVP 不含退款。金额单位统一为「分」。
-- biz_id 为雪花 ID，同时作为渠道 out_trade_no/out_order_no（天然幂等）。

-- ============================================================
-- 1. 支付域：支付单（收款流水）
-- ============================================================

DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order` (
  `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`                 VARCHAR(32)  NOT NULL COMMENT '业务主键（雪花ID），同时作为渠道 out_trade_no',
  `channel`                VARCHAR(16)  NOT NULL DEFAULT 'WECHAT' COMMENT '支付渠道：WECHAT（后续 ALIPAY 等）',
  `biz_type`               VARCHAR(32)  NOT NULL DEFAULT 'TOURNAMENT_ENTRY_FEE' COMMENT '业务类型：TOURNAMENT_ENTRY_FEE 赛事报名费（后续可扩展）',
  `ref_biz_id`             VARCHAR(32)  NOT NULL COMMENT '关联业务ID：TOURNAMENT_ENTRY_FEE 为 tournament_entry.biz_id',
  `payer_user_id`          VARCHAR(32)  NOT NULL COMMENT '付款人 user_id（参与者）',
  `base_amount`            INT          NOT NULL COMMENT '应收金额（分）',
  `fee_amount`             INT          NOT NULL DEFAULT 0 COMMENT '手续费（分）= ceil(base_amount * fee_rate)，用户承担',
  `pay_amount`             INT          NOT NULL COMMENT '实付金额（分）= base_amount + fee_amount',
  `status`                 VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态机：PENDING/PAID/CLOSED/FAILED',
  `channel_transaction_id` VARCHAR(64)  DEFAULT NULL COMMENT '渠道支付流水号（微信 transaction_id）',
  `prepay_id`              VARCHAR(64)  DEFAULT NULL COMMENT '渠道预支付ID（微信 prepay_id）',
  `prepay_expire_time`     DATETIME     DEFAULT NULL COMMENT '预支付凭证有效期（微信 prepay_id 2小时）；未过期可复用不重新下单',
  `active_ref_key`         VARCHAR(128) DEFAULT NULL COMMENT '活跃标识位：活跃(PENDING/PAID)时=biz_type:ref_biz_id:payer_user_id，关闭/失败时置 NULL；承载幂等唯一性',
  `description`            VARCHAR(255) DEFAULT NULL COMMENT '商品描述/备注',
  `pay_time`               DATETIME     DEFAULT NULL COMMENT '支付成功时间',
  `expire_time`            DATETIME     DEFAULT NULL COMMENT '支付超时时间，NULL=不超时（默认）；配置了超时分钟数才写入',
  `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_active_ref` (`active_ref_key`) COMMENT '活跃标识位唯一：同一用户对同一业务至多一条活跃单；多 NULL 共存允许关单后重建',
  KEY `idx_payer` (`payer_user_id`) COMMENT '查我的待付/已付',
  KEY `idx_status_expire` (`status`, `expire_time`) COMMENT '超时未付订单扫描',
  KEY `idx_txn` (`channel_transaction_id`) COMMENT '回调按渠道流水号反查'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单/收款流水（赛事报名费）';

-- ============================================================
-- 2. 支付域：支付全链路留痕（建单/下单/回调，对账与排查用，非聚合根）
-- ============================================================

DROP TABLE IF EXISTS `payment_log`;
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

-- 配置项默认值统一在 com.rally.domain.system.enums.SystemConfigKey 枚举维护，不落 sys_config 表：
-- payment.wechat.fee_rate(0.006) / payment.wechat.fee_desc / payment.pay_timeout_minutes(0)
-- 仅当需覆盖默认值时才在 sys_config 落库对应 config_key。
