package com.rally.domain.translation.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Objects;


@Data
@AllArgsConstructor
public class TranslationKey {

    private TranslationEntityTypeEnum entityType;
    private String originalText;
    private TranslationLanguageEnum language;

    private String buildKey() {
        return String.format("%s:%s:%s",  originalText, entityType, language);
    }

//    @Override
//    public boolean equals(Object o) {
//        if (o == null || getClass() != o.getClass()) return false;
//        TranslationKey that = (TranslationKey) o;
//        return entityType == that.entityType && Objects.equals(originalText.toLowerCase(), that.originalText.toLowerCase()) && language == that.language;
//    }

//    @Override
//    public int hashCode() {
//        return Objects.hash(entityType, originalText.toLowerCase(), language);
//    }
}
