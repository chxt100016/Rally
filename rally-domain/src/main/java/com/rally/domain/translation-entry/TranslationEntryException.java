package com.rally.domain.content.translationentry;

/**
 * 翻译条目聚合拒绝命令时抛出的领域异常。
 */
public final class TranslationEntryException extends RuntimeException {

    private final TranslationEntryError error;

    public TranslationEntryException(TranslationEntryError error) {
        super(error.name());
        this.error = error;
    }

    public TranslationEntryError getError() {
        return error;
    }
}
