package com.rally.protourdata.playerquery.activity;

import com.rally.domain.content.translationentry.RegisterTranslationEntryCmd;
import com.rally.domain.content.translationentry.TranslationEntry;
import com.rally.domain.content.translationentry.TranslationEntryState;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 业务活动 register-missing-tour-translations：逐项尽力登记排名球员姓名的简中缺译键。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMissingTourTranslationsActivity {

    private static final TranslationEntityTypeEnum ENTITY_TYPE = TranslationEntityTypeEnum.PLAYER;
    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TranslationRepository translationRepository;

    public void execute(Set<TranslationKey> missingTranslationKeys) {
        // A1：按 PLAYER/原文/ZH_CN 规范化并去重；空集合不访问仓储。
        Set<TranslationKey> normalized = normalizeAndDeduplicate(missingTranslationKeys);
        if (normalized.isEmpty()) {
            return;
        }

        // A2/A3：每个键独立登记和容错，已存在记录不被覆盖，查询结果继续保留原姓名。
        for (TranslationKey key : normalized) {
            registerOne(key);
        }
    }

    private Set<TranslationKey> normalizeAndDeduplicate(Set<TranslationKey> keys) {
        if (keys == null || keys.isEmpty()) {
            return Set.of();
        }

        Set<TranslationKey> normalized = new LinkedHashSet<>();
        for (TranslationKey key : keys) {
            if (key == null
                    || key.getEntityType() != ENTITY_TYPE
                    || key.getLanguage() != TARGET_LANGUAGE) {
                continue;
            }
            String originalText = key.getOriginalText();
            normalized.add(new TranslationKey(
                    ENTITY_TYPE,
                    originalText == null ? null : originalText.strip(),
                    TARGET_LANGUAGE));
        }
        return normalized;
    }

    private void registerOne(TranslationKey key) {
        try {
            RegisterTranslationEntryCmd command = new RegisterTranslationEntryCmd(
                    ENTITY_TYPE, key.getOriginalText(), TARGET_LANGUAGE);

            // 先经 C1 校验并规范化翻译键；空原文等非法单项不触发仓储。
            TranslationEntry pending = TranslationEntry.register(command);
            TranslationKey normalizedKey = new TranslationKey(
                    pending.key().getEntityType(),
                    pending.key().getOriginalText(),
                    pending.key().getLanguage());
            TranslationData existing = findExisting(normalizedKey);

            // C1 对相同键幂等；无论待译或已翻译，均保留已提交状态。
            TranslationEntry entry = TranslationEntry.register(command, toState(existing));
            if (existing == null) {
                translationRepository.save(toData(entry.state()));
            }
        } catch (DuplicateKeyException exception) {
            log.warn("并发登记排名球员缺译键冲突，按已存在处理: originalText={}",
                    key.getOriginalText());
        } catch (Exception exception) {
            log.error("登记排名球员缺译键失败，继续处理后续姓名: originalText={}",
                    key.getOriginalText(), exception);
        }
    }

    private TranslationData findExisting(TranslationKey key) {
        TranslationData query = new TranslationData()
                .setEntityType(key.getEntityType())
                .setOriginalText(key.getOriginalText())
                .setLanguage(key.getLanguage());
        List<TranslationData> records = translationRepository.findBatch(List.of(query));
        return records == null || records.isEmpty() ? null : records.get(0);
    }

    private TranslationEntryState toState(TranslationData data) {
        if (data == null) {
            return null;
        }
        return new TranslationEntryState(
                data.getId(),
                data.getEntityType(),
                data.getOriginalText(),
                data.getLanguage(),
                data.getTranslatedText(),
                null,
                null);
    }

    private TranslationData toData(TranslationEntryState state) {
        return new TranslationData()
                .setId(state.getId())
                .setEntityType(state.getEntityType())
                .setOriginalText(state.getOriginalText())
                .setLanguage(state.getLanguage())
                .setTranslatedText(state.getTranslatedText());
    }
}
