package com.rally.domain.content.translationentry;

/**
 * 翻译条目聚合的不变量错误标识。
 */
public enum TranslationEntryError {
    TRANSLATION_KEY_CONFLICT,
    TRANSLATION_KEY_IMMUTABLE,
    TRANSLATION_TEXT_INVALID
}
