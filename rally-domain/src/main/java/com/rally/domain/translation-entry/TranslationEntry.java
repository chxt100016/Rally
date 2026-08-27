package com.rally.domain.content.translationentry;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;

import java.util.Objects;

/**
 * 翻译条目聚合根。translation 表的状态只能经由本聚合的 C1/C2 命令建立或改写。
 */
public final class TranslationEntry {

    private static final int TRANSLATED_TEXT_MAX_LENGTH = 500;

    private TranslationEntryState state;
    private final TranslationEntryKey identity;

    private TranslationEntry(TranslationEntryState state, TranslationEntryKey identity) {
        this.state = state;
        this.identity = identity;
    }

    /**
     * 从已提交记录重建聚合；缺失记录对应 C2 的“条目不存在”拒绝情形。
     */
    public static TranslationEntry restore(TranslationEntryState state) {
        if (state == null) {
            throw new BusinessException(BizErrorCode.DATA_NOT_FOUND);
        }
        TranslationEntryKey key = TranslationEntryKey.of(
                state.getEntityType(), state.getOriginalText(), state.getLanguage());
        TranslationEntry entry = new TranslationEntry(state, key);
        entry.checkInvariants();
        return entry;
    }

    /**
     * C1：登记新的待译条目。
     */
    public static TranslationEntry register(RegisterTranslationEntryCmd command) {
        return register(command, null);
    }

    /**
     * C1：登记待译条目；仓储已查到相同键时幂等返回该条目并保持原译文。
     * 并发登记由 translation 的组合唯一索引裁决，未胜出的调用方加载胜出记录后
     * 再调用本重载即可收敛为相同结果。
     */
    public static TranslationEntry register(RegisterTranslationEntryCmd command,
                                            TranslationEntryState existing) {
        if (command == null) {
            throw new TranslationEntryException(TranslationEntryError.TRANSLATION_KEY_CONFLICT);
        }
        TranslationEntryKey requestedKey = TranslationEntryKey.of(
                command.getEntityType(), command.getOriginalText(), command.getLanguage());

        if (existing != null) {
            TranslationEntry entry = restore(existing);
            if (!entry.identity.equals(requestedKey)) {
                throw new TranslationEntryException(TranslationEntryError.TRANSLATION_KEY_CONFLICT);
            }
            entry.checkInvariants();
            return entry;
        }

        TranslationEntryState pending = new TranslationEntryState(
                null,
                requestedKey.getEntityType(),
                requestedKey.getOriginalText(),
                requestedKey.getLanguage(),
                "",
                null,
                null);
        TranslationEntry entry = new TranslationEntry(pending, requestedKey);
        entry.checkInvariants();
        return entry;
    }

    /**
     * C2：保存规范化译文。PENDING 与 TRANSLATED 都允许调用，重复保存保持幂等。
     */
    public void saveTranslation(SaveTranslationTextCmd command) {
        if (command == null
                || command.getId() == null
                || state.getId() == null
                || !Objects.equals(command.getId(), state.getId())) {
            throw new BusinessException(BizErrorCode.DATA_NOT_FOUND);
        }

        String normalizedText = normalize(command.getTranslatedText());
        if (normalizedText.isEmpty() || length(normalizedText) > TRANSLATED_TEXT_MAX_LENGTH) {
            throw new TranslationEntryException(TranslationEntryError.TRANSLATION_TEXT_INVALID);
        }

        state = state.withTranslatedText(normalizedText);
        checkInvariants();
    }

    /**
     * 聚合当前不可变状态，供仓储保存。
     */
    public TranslationEntryState state() {
        return state;
    }

    public TranslationEntryKey key() {
        return identity;
    }

    public TranslationEntryStatus status() {
        return normalize(state.getTranslatedText()).isEmpty()
                ? TranslationEntryStatus.PENDING
                : TranslationEntryStatus.TRANSLATED;
    }

    /**
     * I1/I2/I3：每个命令执行后校验相关不变量。
     */
    private void checkInvariants() {
        TranslationEntryKey currentKey = TranslationEntryKey.of(
                state.getEntityType(), state.getOriginalText(), state.getLanguage());
        if (!identity.equals(currentKey)) {
            throw new TranslationEntryException(TranslationEntryError.TRANSLATION_KEY_IMMUTABLE);
        }
        if (!Objects.equals(state.getOriginalText(), currentKey.getOriginalText())) {
            throw new TranslationEntryException(TranslationEntryError.TRANSLATION_KEY_CONFLICT);
        }

        String translatedText = state.getTranslatedText();
        TranslationEntryStatus currentStatus = status();
        if (currentStatus == TranslationEntryStatus.PENDING) {
            if (translatedText == null || !translatedText.isEmpty()) {
                throw new TranslationEntryException(TranslationEntryError.TRANSLATION_TEXT_INVALID);
            }
            return;
        }

        String normalizedText = normalize(translatedText);
        if (!Objects.equals(translatedText, normalizedText)
                || normalizedText.isEmpty()
                || length(normalizedText) > TRANSLATED_TEXT_MAX_LENGTH) {
            throw new TranslationEntryException(TranslationEntryError.TRANSLATION_TEXT_INVALID);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.strip();
    }

    private static int length(String value) {
        return value.codePointCount(0, value.length());
    }
}
