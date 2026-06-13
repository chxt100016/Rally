-- ============================================================
-- 用户登录注册系统建表语句（MySQL 8.0+）
-- ============================================================




-- 用户核心表
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id     VARCHAR(32)  NOT NULL COMMENT '系统唯一 ID（雪花算法字符串形式）',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar_url  VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    gender      VARCHAR(16) NOT NULL DEFAULT 'UNDISCLOSED' COMMENT '性别',
    birthday    DATE         DEFAULT NULL COMMENT '生日，用于年龄段筛选',
    bio         VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    city_code   VARCHAR(32)  DEFAULT NULL COMMENT '用户当前城市编码',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '手机号（MVP 不收集，列保留供后续手机号注册使用）',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱（MVP 不收集，列保留）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户核心表';

-- 渠道认证表
DROP TABLE IF EXISTS account;
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
