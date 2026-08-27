package com.rally.protourdata.playertournamentpathquery.activity;

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
 * 业务活动 register-missing-tour-translations：逐项尽力登记球员赛事路径的简中缺译键。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMissingTourTranslationsActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;
    private static final Set<TranslationEntityTypeEnum> SUPPORTED_ENTITY_TYPES = Set.of(
            TranslationEntityTypeEnum.PLAYER,
            TranslationEntityTypeEnum.COURT);

    private final TranslationRepository translationRepository;

    public void execute(Set<TranslationKey> missingTranslationKeys) {
        // A1：只保留路径展示所需的简中球员/球场键，按完整翻译键去重。
        Set<TranslationKey> normalized = normalizeAndDeduplicate(missingTranslationKeys);
        if (normalized.isEmpty()) {
            return;
        }

        // A2/A3：每个键独立登记与容错，已存在记录不覆盖，路径继续保留原文。
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
                    || !SUPPORTED_ENTITY_TYPES.contains(key.getEntityType())
                    || key.getLanguage() != TARGET_LANGUAGE
                    || key.getOriginalText() == null
                    || key.getOriginalText().isBlank()) {
                continue;
            }
            normalized.add(new TranslationKey(
                    key.getEntityType(), key.getOriginalText().strip(), TARGET_LANGUAGE));
        }
        return normalized;
    }

    private void registerOne(TranslationKey key) {
        try {
            RegisterTranslationEntryCmd command = new RegisterTranslationEntryCmd(
                    key.getEntityType(), key.getOriginalText(), key.getLanguage());
            TranslationData existing = findExisting(key);

            // @content.translation-entry C1 创建空译文 PENDING；已有键幂等成功且保留现有译文。
            TranslationEntry entry = TranslationEntry.register(command, toState(existing));
            if (existing == null) {
                translationRepository.save(toData(entry.state()));
            }
        } catch (DuplicateKeyException exception) {
            // 并发唯一键竞争视为本项已登记，读请求继续返回原文。
            log.warn("并发登记球员路径缺译键冲突，按已存在处理: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage());
        } catch (Exception exception) {
            // 单项查询或保存失败只记录日志，已成功项不回滚，后续键继续。
            log.error("登记球员路径缺译键失败: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage(), exception);
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
