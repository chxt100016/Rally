package com.rally.domain.content.translationentry;

import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;

import java.util.Objects;

/**
 * 由实体类型、规范化原文和目标语言组成的不可变翻译键。
 */
public final class TranslationEntryKey {

    private static final int ORIGINAL_TEXT_MAX_LENGTH = 200;

    private final TranslationEntityTypeEnum entityType;
    private final String originalText;
    private final TranslationLanguageEnum language;

    private TranslationEntryKey(TranslationEntityTypeEnum entityType,
                                String originalText,
                                TranslationLanguageEnum language) {
        this.entityType = entityType;
        this.originalText = originalText;
        this.language = language;
    }

    /**
     * I1：建立规范化且合法的翻译键。枚举类型本身即为项目支持集合。
     */
    public static TranslationEntryKey of(TranslationEntityTypeEnum entityType,
                                         String originalText,
                                         TranslationLanguageEnum language) {
        String normalizedText = normalize(originalText);
        if (entityType == null
                || language == null
                || normalizedText.isEmpty()
                || length(normalizedText) > ORIGINAL_TEXT_MAX_LENGTH) {
            throw new TranslationEntryException(TranslationEntryError.TRANSLATION_KEY_CONFLICT);
        }
        return new TranslationEntryKey(entityType, normalizedText, language);
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

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TranslationEntryKey that)) {
            return false;
        }
        return entityType == that.entityType
                && Objects.equals(originalText, that.originalText)
                && language == that.language;
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityType, originalText, language);
    }
}
