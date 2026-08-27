package com.rally.domain.content.translationentry;

import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationLanguageEnum;

/**
 * C1 登记待译条目的命令入参。
 */
public final class RegisterTranslationEntryCmd {

    private final TranslationEntityTypeEnum entityType;
    private final String originalText;
    private final TranslationLanguageEnum language;

    public RegisterTranslationEntryCmd(TranslationEntityTypeEnum entityType,
                                       String originalText,
                                       TranslationLanguageEnum language) {
        this.entityType = entityType;
        this.originalText = originalText;
        this.language = language;
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
}
