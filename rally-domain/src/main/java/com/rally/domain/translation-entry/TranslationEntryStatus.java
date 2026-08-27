package com.rally.domain.content.translationentry;

/**
 * 翻译条目状态；状态由译文字段推导，不单独持久化。
 */
public enum TranslationEntryStatus {
    PENDING,
    TRANSLATED
}
