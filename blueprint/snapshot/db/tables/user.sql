CREATE TABLE user (
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    user_id     VARCHAR(32)  NOT NULL COMMENT '系统唯一 ID（雪花算法字符串形式）',
    nickname    VARCHAR(64)  DEFAULT NULL COMMENT '昵称',
    avatar_url  VARCHAR(512) DEFAULT NULL COMMENT '头像 URL',
    gender      VARCHAR(16) NOT NULL DEFAULT 'UNDISCLOSED' COMMENT '性别',
    birthday    DATE         DEFAULT NULL COMMENT '生日，用于年龄段筛选',
    bio         VARCHAR(255) DEFAULT NULL COMMENT '个人简介',
    city_code   VARCHAR(32)  DEFAULT NULL COMMENT '用户当前城市编码',
    phone       VARCHAR(20)  DEFAULT NULL COMMENT '微信授权手机号',
    email       VARCHAR(100) DEFAULT NULL COMMENT '邮箱（MVP 不收集，列保留）',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户核心表';
