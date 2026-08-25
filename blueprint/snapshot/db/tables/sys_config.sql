CREATE TABLE `sys_config` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`       VARCHAR(32)  NOT NULL COMMENT '雪花 ID（业务主键）',
  `config_key`   VARCHAR(128) NOT NULL COMMENT '配置键，规范 domain.module.field，如 score.calibration.deviation_t1',
  `config_value` VARCHAR(2048) NOT NULL COMMENT '字符串化值；json 类型存序列化串',
  `value_type`   varchar(8) NOT NULL DEFAULT 'string' COMMENT '值类型，决定读取时的解析方式',
  `scope`        VARCHAR(64)  NOT NULL DEFAULT 'global' COMMENT '作用域：global 或 city:{cityCode} 等',
  `description`  VARCHAR(255) DEFAULT NULL COMMENT '中文说明',
  `enabled`      TINYINT(1)   NOT NULL DEFAULT 1 COMMENT '是否启用：1 启用，0 停用（停用则读取回退默认值）',
  `version`      INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本号，便于变更日志与并发写',
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  UNIQUE KEY `uk_key_scope` (`config_key`, `scope`),
  KEY `idx_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='全局配置表';
