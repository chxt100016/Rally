package com.rally.domain.translation.model;

import lombok.Data;

@Data
public class TranslationData {

    private Long id;
    private TranslationEntityTypeEnum entityType;
    private String originalText;
    private TranslationLanguageEnum language;
    /** null 表示待翻译 */
    private String translatedText;
}
