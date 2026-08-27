package com.rally.contentproduction.dailyschedulecopy.activity;

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
 * 业务活动 register-missing-translations：逐项尽力登记当次文案的缺译键。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMissingTranslationsActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;
    private static final Set<TranslationEntityTypeEnum> SUPPORTED_ENTITY_TYPES = Set.of(
            TranslationEntityTypeEnum.TOURNAMENT,
            TranslationEntityTypeEnum.COURT,
            TranslationEntityTypeEnum.PLAYER);

    private final TranslationRepository translationRepository;

    public void execute(Set<TranslationKey> missingTranslationKeys) {
        // A1 只保留三种内容实体的非空简体中文翻译键，先规范化原文再去重。
        for (TranslationKey key : normalizeAndDeduplicate(missingTranslationKeys)) {
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

            // A2 已有条目经 C1 重建后幂等成功，不会清空已有译文。
            TranslationEntry entry = TranslationEntry.register(command, toState(existing));
            if (existing == null) {
                translationRepository.save(toData(entry.state()));
            }
        } catch (DuplicateKeyException e) {
            // A3 并发登记的唯一键冲突表示本项已由其他请求登记。
            log.warn("并发登记缺译键冲突，按已存在处理: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage());
        } catch (Exception e) {
            // A3 单项失败只记录日志，继续处理其他键，不影响本次文案交付。
            log.error("登记缺译键失败: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage(), e);
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
