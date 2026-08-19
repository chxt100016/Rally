DROP TABLE IF EXISTS `user_behavior_log`;
CREATE TABLE `user_behavior_log` (
  `id`               BIGINT UNSIGNED   NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `user_id`          VARCHAR(32)        DEFAULT NULL COMMENT '用户 ID，匿名请求为空',
  `request_id`       VARCHAR(64)        DEFAULT NULL COMMENT '本次行为采集请求 ID',
  `http_method`      VARCHAR(10)        NOT NULL COMMENT 'HTTP 方法',
  `request_uri`      VARCHAR(512)       NOT NULL COMMENT '请求路径，不含 query string',
  `route_pattern`    VARCHAR(255)       DEFAULT NULL COMMENT 'Spring 路由模板',
  `request_params`   JSON               DEFAULT NULL COMMENT 'path/query/body 原始请求入参',
  `params_truncated` TINYINT(1)         NOT NULL DEFAULT 0 COMMENT '参数是否因超过限制被省略',
  `client_ip`        VARCHAR(45)        DEFAULT NULL COMMENT '客户端 IPv4/IPv6',
  `user_agent`       VARCHAR(512)       DEFAULT NULL COMMENT '客户端 User-Agent',
  `http_status`      SMALLINT UNSIGNED  NOT NULL COMMENT 'HTTP 状态码',
  `duration_ms`      INT UNSIGNED       NOT NULL DEFAULT 0 COMMENT '接口耗时毫秒',
  `exception_type`   VARCHAR(255)       DEFAULT NULL COMMENT '未处理异常类型',
  `occurred_at`      DATETIME(3)        NOT NULL COMMENT '请求发生时间',
  `create_time`      DATETIME(3)        NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '入库时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `occurred_at`),
  KEY `idx_route_time` (`route_pattern`, `occurred_at`),
  KEY `idx_occurred_at` (`occurred_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户端用户接口行为日志';
