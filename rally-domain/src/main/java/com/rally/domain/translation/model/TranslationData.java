package com.rally.domain.translation.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class TranslationData {

    private Long id;
    private TranslationEntityTypeEnum entityType;
    private String originalText;
    private TranslationLanguageEnum language;
    /** null 表示待翻译 */
    private String translatedText;
}
