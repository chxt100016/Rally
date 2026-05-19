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

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        TranslationKey that = (TranslationKey) o;
        String thisText = originalText != null ? originalText.toLowerCase() : null;
        String thatText = that.originalText != null ? that.originalText.toLowerCase() : null;
        return entityType == that.entityType && Objects.equals(thisText, thatText) && language == that.language;
    }

    @Override
    public int hashCode() {
        String lowerText = originalText != null ? originalText.toLowerCase() : null;
        return Objects.hash(entityType, lowerText, language);
    }
}
