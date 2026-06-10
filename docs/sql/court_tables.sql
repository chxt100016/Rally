-- Rally 球场信息表建表语句

-- ============================================================
-- 1. 球场域：球场信息表
-- ============================================================

DROP TABLE IF EXISTS `rally_court`;
CREATE TABLE `rally_court` (
  `id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  `biz_id`        VARCHAR(32)  NOT NULL COMMENT '雪花 ID（业务主键）',
  `name`          VARCHAR(128) NOT NULL COMMENT '球场名称',
  `address`       VARCHAR(256) DEFAULT NULL COMMENT '球场地址',
  `lng`           DOUBLE       NOT NULL COMMENT '经度',
  `lat`           DOUBLE       NOT NULL COMMENT '纬度',
  `city_code`     VARCHAR(32)  NOT NULL COMMENT '城市编码',
  `district_code` VARCHAR(32)  DEFAULT NULL COMMENT '区县编码',
  `total`         INT          DEFAULT NULL COMMENT '场地数量',
  `remark`        VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `create_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time`   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_biz_id` (`biz_id`),
  KEY `idx_city_code` (`city_code`),
  KEY `idx_city_district` (`city_code`, `district_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='球场信息表';
