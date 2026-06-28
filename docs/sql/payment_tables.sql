-- Rally 支付域建表语句（MVP：收款 + 分账 + 对账）
-- 范围：活动结束后发起人「发起收款」→ 参与者微信支付 → 分账给发起人。
-- MVP 不含退款（后付费模式无预收，无需退款）。金额单位统一为「分」。
-- biz_id 为雪花 ID，同时作为渠道 out_trade_no/out_order_no（天然幂等）。

-- ============================================================
-- 1. 支付域：支付单（收款流水）
-- ============================================================

DROP TABLE IF EXISTS `payment_order`;
CREATE TABLE `payment_order` (
  `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`                 VARCHAR(32)  NOT NULL COMMENT '业务主键（雪花ID），同时作为渠道 out_trade_no',
  `channel`                VARCHAR(16)  NOT NULL DEFAULT 'WECHAT' COMMENT '支付渠道：WECHAT（后续 ALIPAY 等）',
  `biz_type`               VARCHAR(32)  NOT NULL DEFAULT 'MEETUP_COLLECT' COMMENT '业务类型：MEETUP_COLLECT 活动收款（后续可扩展）',
  `collection_batch_id`    VARCHAR(32)  NOT NULL COMMENT '收款批次ID（一次「发起收款」生成一批支付单，共用同一批次）',
  `meetup_id`              VARCHAR(32)  NOT NULL COMMENT '关联 rally_meetup.biz_id',
  `payer_user_id`          VARCHAR(32)  NOT NULL COMMENT '付款人 user_id（参与者）',
  `payee_user_id`          VARCHAR(32)  NOT NULL COMMENT '收款受益人 user_id（发起人），分账目标',
  `payee_account`          VARCHAR(64)  DEFAULT NULL COMMENT '收款受益人渠道账号（微信 openid），冗余避免分账时再次反查',
  `base_amount`            INT          NOT NULL COMMENT '应收本金（分），分账基准',
  `fee_amount`             INT          NOT NULL DEFAULT 0 COMMENT '手续费（分）= ceil(base_amount * fee_rate)，用户承担',
  `pay_amount`             INT          NOT NULL COMMENT '实付金额（分）= base_amount + fee_amount',
  `status`                 VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态机：PENDING/PAID/CLOSED/FAILED',
  `channel_transaction_id` VARCHAR(64)  DEFAULT NULL COMMENT '渠道支付流水号（微信 transaction_id），分账依赖',
  `prepay_id`              VARCHAR(64)  DEFAULT NULL COMMENT '渠道预支付ID（微信 prepay_id）',
  `description`            VARCHAR(255) DEFAULT NULL COMMENT '商品描述/备注',
  `pay_time`               DATETIME     DEFAULT NULL COMMENT '支付成功时间',
  `expire_time`            DATETIME     DEFAULT NULL COMMENT '支付超时时间，NULL=不超时（默认）；配置了超时分钟数才写入',
  `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_batch_payer` (`collection_batch_id`, `payer_user_id`) COMMENT '同一批次同一付款人只一单（防重复发起收款）',
  KEY `idx_meetup_status` (`meetup_id`, `status`) COMMENT '详情页收款进度/批量查询',
  KEY `idx_payer` (`payer_user_id`) COMMENT '查我的待付/已付',
  KEY `idx_status_expire` (`status`, `expire_time`) COMMENT '超时未付订单扫描',
  KEY `idx_txn` (`channel_transaction_id`) COMMENT '回调按渠道流水号反查'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='支付单/收款流水（活动后向参与者收款）';

-- ============================================================
-- 2. 支付域：分账单（针对单笔支付交易向发起人分账）
-- ============================================================

DROP TABLE IF EXISTS `payment_settlement`;
CREATE TABLE `payment_settlement` (
  `id`                     BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`                 VARCHAR(32)  NOT NULL COMMENT '业务主键（雪花ID），同时作为渠道 out_order_no',
  `payment_order_id`       VARCHAR(32)  NOT NULL COMMENT '关联 payment_order.biz_id（分账针对单笔交易）',
  `channel`                VARCHAR(16)  NOT NULL DEFAULT 'WECHAT' COMMENT '支付渠道',
  `channel_transaction_id` VARCHAR(64)  NOT NULL COMMENT '被分账的渠道支付流水号（微信 transaction_id）',
  `meetup_id`              VARCHAR(32)  NOT NULL COMMENT '冗余约球ID',
  `payee_user_id`          VARCHAR(32)  NOT NULL COMMENT '受益人 user_id（发起人）',
  `payee_account`          VARCHAR(64)  DEFAULT NULL COMMENT '受益人分账接收方标识（个人 openid）',
  `share_amount`           INT          NOT NULL COMMENT '分账金额（分）= 该笔订单应收本金',
  `status`                 VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT '状态机：PENDING/PROCESSING/FINISHED/FAILED',
  `channel_order_id`       VARCHAR(64)  DEFAULT NULL COMMENT '渠道分账单号（微信 order_id）',
  `fail_reason`            VARCHAR(255) DEFAULT NULL COMMENT '失败原因',
  `finish_time`            DATETIME     DEFAULT NULL COMMENT '分账完成时间',
  `create_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_payment_order` (`payment_order_id`) COMMENT '单笔交易仅分账一次（幂等）',
  KEY `idx_meetup_status` (`meetup_id`, `status`) COMMENT '按活动查分账',
  KEY `idx_status` (`status`) COMMENT '分账中订单对账扫描'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账单（收款成功后按笔向发起人分账）';

-- ============================================================
-- 3. 支付域：支付全链路留痕（建单/下单/回调，对账与排查用，非聚合根）
-- ============================================================

DROP TABLE IF EXISTS `payment_log`;
CREATE TABLE `payment_log` (
  `id`             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`         VARCHAR(32)  NOT NULL COMMENT '雪花ID',
  `channel`        VARCHAR(16)  NOT NULL DEFAULT 'WECHAT' COMMENT '支付渠道',
  `log_type`       VARCHAR(24)  NOT NULL COMMENT '日志类型：COLLECT 发起收款建单 / PREPAY 参与人发起支付下单 / CALLBACK 渠道回调',
  `ref_type`       VARCHAR(16)  DEFAULT NULL COMMENT '关联单类型：ORDER 支付单 / SETTLEMENT 分账单（CALLBACK 区分支付/分账回调）',
  `ref_id`         VARCHAR(64)  DEFAULT NULL COMMENT '关联单号（payment_order/payment_settlement biz_id，即渠道 out_trade_no/out_order_no）',
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

-- ============================================================
-- 4. 支付域：分账接收方账本（我方维护，微信需先「添加接收方」才能分账，且有数量上限）
-- ============================================================

DROP TABLE IF EXISTS `payment_share_receiver`;
CREATE TABLE `payment_share_receiver` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`           VARCHAR(32)  NOT NULL COMMENT '雪花ID',
  `channel`          VARCHAR(16)  NOT NULL DEFAULT 'WECHAT' COMMENT '支付渠道',
  `user_id`          VARCHAR(32)  NOT NULL COMMENT '受益人 user_id（发起人）',
  `account_type`     VARCHAR(24)  NOT NULL DEFAULT 'PERSONAL_OPENID' COMMENT '接收方类型（对齐微信 receiver.type）：PERSONAL_OPENID 个人',
  `account`          VARCHAR(64)  NOT NULL COMMENT '接收方标识（本小程序 appid 下的 openid）',
  `bind_status`      VARCHAR(16)  NOT NULL DEFAULT 'BOUND' COMMENT '绑定状态：BOUND 已添加 / UNBOUND 已删除（淘汰）',
  `bound_time`       DATETIME     DEFAULT NULL COMMENT '添加为分账接收方时间',
  `last_share_time`  DATETIME     DEFAULT NULL COMMENT '最近活跃时间（取发起收款时间），LRU 淘汰依据',
  `unbind_time`      DATETIME     DEFAULT NULL COMMENT '删除（淘汰）时间',
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_channel_account` (`channel`, `account`) COMMENT '同渠道同接收方唯一（幂等 upsert）',
  KEY `idx_user` (`channel`, `user_id`) COMMENT '按受益人查',
  KEY `idx_lru` (`bind_status`, `last_share_time`) COMMENT '接近上限时按最久未用淘汰扫描'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分账接收方账本（绑定状态 + LRU 淘汰依据）';

-- 配置项默认值统一在 com.rally.domain.system.enums.SystemConfigKey 枚举维护，不落 sys_config 表：
-- payment.wechat.fee_rate(0.006) / payment.wechat.fee_desc / payment.pay_timeout_minutes(0) / payment.wechat.share_receiver_max(20000)
-- 仅当需覆盖默认值时才在 sys_config 落库对应 config_key。
