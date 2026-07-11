-- 用户扩展信息竖表
-- 采用 EAV 模式，通过 ext_key/ext_value 字段灵活扩展用户属性
-- 首期需求：记录用户微信付款码

DROP TABLE IF EXISTS user_ext;
CREATE TABLE user_ext (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    biz_id      VARCHAR(32)  NOT NULL COMMENT '业务唯一 ID（雪花算法字符串）',
    user_id     VARCHAR(32)  NOT NULL COMMENT '关联 user.user_id',
    ext_key     VARCHAR(64)  NOT NULL COMMENT '扩展字段 key（如 wechat_payment_code）',
    ext_value   TEXT         DEFAULT NULL COMMENT '扩展字段 value（存储付款码 URL 或 base64）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_biz_id (biz_id),
    UNIQUE KEY uk_user_ext_key (user_id, ext_key) COMMENT '保证同一用户的同一 key 只有一条记录',
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户扩展信息竖表';
