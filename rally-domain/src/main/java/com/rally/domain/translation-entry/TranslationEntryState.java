package com.rally.domain.content.translationentry;

import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;

import java.time.LocalDateTime;

/**
 * translation 表的一条不可变领域状态。数据库负责生成 id 和维护时间字段。
 */
public final class TranslationEntryState {

    private final Long id;
    private final TranslationEntityTypeEnum entityType;
    private final String originalText;
    private final TranslationLanguageEnum language;
    private final String translatedText;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    public TranslationEntryState(Long id,
                                 TranslationEntityTypeEnum entityType,
                                 String originalText,
                                 TranslationLanguageEnum language,
                                 String translatedText,
                                 LocalDateTime createTime,
                                 LocalDateTime updateTime) {
        this.id = id;
        this.entityType = entityType;
        this.originalText = originalText;
        this.language = language;
        this.translatedText = translatedText;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public TranslationEntityTypeEnum getEntityType() {
        return entityType;
    }

    public String getOriginalText() {
        return originalText;
    }

    public TranslationLanguageEnum getLanguage() {
        return language;
    }

    public String getTranslatedText() {
        return translatedText;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    TranslationEntryState withTranslatedText(String value) {
        return new TranslationEntryState(
                id, entityType, originalText, language, value, createTime, updateTime);
    }
}
