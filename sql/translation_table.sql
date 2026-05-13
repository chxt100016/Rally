CREATE TABLE `translation`
(
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `entity_type`     VARCHAR(50)  NOT NULL COMMENT '实体类型枚举值，如 COURT、PLAYER',
    `original_text`   VARCHAR(500) NOT NULL COMMENT '原始文案',
    `language`        VARCHAR(20)  NOT NULL COMMENT '目标语言枚举值，如 ZH_CN',
    `translated_text` VARCHAR(500) DEFAULT NULL COMMENT 'NULL 表示待翻译',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_entity_text_lang` (`entity_type`, `original_text` (200), `language`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='翻译表';
