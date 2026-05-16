-- ============================================================
-- 翻译缓存表（MySQL 8.0+）
-- 存储实体字段的多语言翻译结果，避免重复调用翻译 API
-- ============================================================

DROP TABLE IF EXISTS translation;
CREATE TABLE translation (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    entity_type     VARCHAR(50)  NOT NULL COMMENT '实体类型，如 tournament_name / player_name',
    original_text   VARCHAR(500) NOT NULL COMMENT '原始文本',
    language        VARCHAR(10)  NOT NULL COMMENT '目标语言，如 zh / en / fr',
    translated_text VARCHAR(500) NOT NULL COMMENT '翻译后的文本',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    -- 同一实体类型 + 原文 + 语言组合唯一，防止重复翻译
    UNIQUE KEY uk_translation_entity_text_lang (entity_type, original_text(200), language),
    INDEX idx_translation_entity_type (entity_type),
    INDEX idx_translation_language    (language)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多语言翻译缓存';
