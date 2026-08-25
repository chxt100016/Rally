CREATE TABLE account (
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    account_id     VARCHAR(32)  NOT NULL COMMENT '账号唯一 ID',
    user_id        VARCHAR(32)  NOT NULL COMMENT '关联 users.user_id',
    channel        ENUM('phone','wechat_miniapp') NOT NULL COMMENT '渠道类型',
    identifier     VARCHAR(128) NOT NULL COMMENT '渠道唯一标识：手机号 或 wechat_openid',
    credential     VARCHAR(256) DEFAULT NULL COMMENT '凭证：密码哈希；微信渠道保持 NULL',
    union_id       VARCHAR(128) DEFAULT NULL COMMENT 'UnionID（微信开放平台、Apple等渠道）',
    create_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '绑定时间',
    update_time    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_account_id (account_id),
    UNIQUE KEY uk_channel_identifier (channel, identifier),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='渠道认证表';
