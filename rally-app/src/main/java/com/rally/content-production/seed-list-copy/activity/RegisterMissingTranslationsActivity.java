package com.rally.contentproduction.seedlistcopy.activity;

import com.rally.domain.content.translationentry.RegisterTranslationEntryCmd;
import com.rally.domain.content.translationentry.TranslationEntry;
import com.rally.domain.content.translationentry.TranslationEntryState;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务活动 register-missing-translations：逐项尽力登记种子名单的球员缺译键。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMissingTranslationsActivity {

    private final TranslationRepository translationRepository;

    public void execute(Set<TranslationKey> missingTranslations) {
        // A1 只接受 PLAYER 的非空缺译键，规范化原文后按完整翻译键去重。
        Set<TranslationKey> normalized = normalizeAndDeduplicate(missingTranslations);
        if (normalized.isEmpty()) {
            return;
        }

        // A2 一次批量查出已有组合，无论是待译还是已翻译都不重复建立。
        Map<TranslationKey, TranslationData> existing = findExisting(normalized);
        for (TranslationKey key : normalized) {
            if (existing.containsKey(key)) {
                continue;
            }
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
                    || key.getEntityType() != TranslationEntityTypeEnum.PLAYER
                    || key.getLanguage() == null
                    || key.getOriginalText() == null
                    || key.getOriginalText().isBlank()) {
                continue;
            }
            normalized.add(new TranslationKey(
                    TranslationEntityTypeEnum.PLAYER,
                    key.getOriginalText().strip(),
                    key.getLanguage()));
        }
        return normalized;
    }

    private Map<TranslationKey, TranslationData> findExisting(Set<TranslationKey> keys) {
        List<TranslationData> queries = keys.stream()
                .map(key -> new TranslationData()
                        .setEntityType(key.getEntityType())
                        .setOriginalText(key.getOriginalText())
                        .setLanguage(key.getLanguage()))
                .toList();
        try {
            List<TranslationData> records = translationRepository.findBatch(queries);
            if (records == null || records.isEmpty()) {
                return Map.of();
            }

            Map<TranslationKey, TranslationData> existing = new LinkedHashMap<>();
            for (TranslationData record : records) {
                if (record == null
                        || record.getEntityType() != TranslationEntityTypeEnum.PLAYER
                        || record.getOriginalText() == null
                        || record.getOriginalText().isBlank()
                        || record.getLanguage() == null) {
                    continue;
                }
                existing.put(new TranslationKey(
                        record.getEntityType(), record.getOriginalText().strip(), record.getLanguage()), record);
            }
            return existing;
        } catch (Exception e) {
            // 查重失败时仍逐项尝试；已有记录最终由唯一键冲突按幂等成功处理。
            log.error("批量查询缺译键失败，将逐项尝试登记: count={}", keys.size(), e);
            return Map.of();
        }
    }

    private void registerOne(TranslationKey key) {
        try {
            // A3 通过 @content.translation-entry C1 登记空译文，单项失败不影响后续键。
            TranslationEntry entry = TranslationEntry.register(new RegisterTranslationEntryCmd(
                    key.getEntityType(), key.getOriginalText(), key.getLanguage()));
            translationRepository.save(toData(entry.state()));
        } catch (DuplicateKeyException e) {
            log.warn("并发登记缺译键冲突，按已存在处理: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage());
        } catch (Exception e) {
            log.error("登记缺译键失败: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage(), e);
        }
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
