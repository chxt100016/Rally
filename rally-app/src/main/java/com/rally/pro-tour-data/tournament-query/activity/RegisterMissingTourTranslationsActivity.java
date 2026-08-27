package com.rally.protourdata.tournamentquery.activity;

import com.rally.domain.content.translationentry.RegisterTranslationEntryCmd;
import com.rally.domain.content.translationentry.TranslationEntry;
import com.rally.domain.content.translationentry.TranslationEntryState;
import com.rally.domain.tour.model.TournamentDTO;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务活动 register-missing-tour-translations：应用赛事目录简中译文并逐项登记缺口。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RegisterMissingTourTranslationsActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TranslationCache translationCache;
    private final TranslationRepository translationRepository;

    public List<TournamentDTO> execute(List<TournamentDTO> tournaments,
                                       Set<TranslationKey> translationKeys) {
        // 空目录不读缓存、不登记缺译项。
        if (CollectionUtils.isEmpty(tournaments)) {
            return tournaments == null ? List.of() : tournaments;
        }

        Map<TranslationKey, List<TournamentDTO>> targets = collectTargets(tournaments);
        Set<TranslationKey> keys = translationKeys == null
                ? Set.of()
                : new LinkedHashSet<>(translationKeys);

        // A1：一次收集去重键后查询 ZH_CN 缓存。查询异常不在单项登记容错范围内。
        Map<TranslationKey, String> translations = new HashMap<>();
        Set<TranslationKey> missing = new LinkedHashSet<>();
        for (TranslationKey key : keys) {
            String translated = translationCache.get(key);
            if (translated == null) {
                missing.add(key);
            } else if (StringUtils.isNotBlank(translated)) {
                translations.put(key, translated);
            }
        }

        // A3：每个未命中键独立登记；重复或任何单项异常都只记录并继续。
        for (TranslationKey key : missing) {
            registerOne(key);
        }

        // A2：只应用非空命中值；未命中或空译文保留原展示文案。
        for (Map.Entry<TranslationKey, String> entry : translations.entrySet()) {
            List<TournamentDTO> items = targets.get(entry.getKey());
            if (items == null) {
                continue;
            }
            switch (entry.getKey().getEntityType()) {
                case TOURNAMENT -> items.forEach(item -> item.setName(entry.getValue()));
                case CITY -> items.forEach(item -> item.setCity(entry.getValue()));
                case SURFACE -> items.forEach(item -> item.setSurfaceLabel(entry.getValue()));
                default -> {
                    // 该活动的键只由赛事名、城市和场地表面组成。
                }
            }
        }
        return tournaments;
    }

    private Map<TranslationKey, List<TournamentDTO>> collectTargets(List<TournamentDTO> tournaments) {
        Map<TranslationKey, List<TournamentDTO>> targets = new HashMap<>();
        for (TournamentDTO tournament : tournaments) {
            addTarget(targets, TranslationEntityTypeEnum.TOURNAMENT, tournament.getName(), tournament);
            addTarget(targets, TranslationEntityTypeEnum.CITY, tournament.getCity(), tournament);
            addTarget(targets, TranslationEntityTypeEnum.SURFACE, tournament.getSurfaceLabel(), tournament);
        }
        return targets;
    }

    private void addTarget(Map<TranslationKey, List<TournamentDTO>> targets,
                           TranslationEntityTypeEnum entityType,
                           String originalText,
                           TournamentDTO tournament) {
        TranslationKey key = new TranslationKey(entityType, originalText, TARGET_LANGUAGE);
        targets.computeIfAbsent(key, ignored -> new ArrayList<>()).add(tournament);
    }

    private void registerOne(TranslationKey key) {
        try {
            RegisterTranslationEntryCmd command = new RegisterTranslationEntryCmd(
                    key.getEntityType(), key.getOriginalText(), key.getLanguage());
            TranslationEntry pending = TranslationEntry.register(command);
            TranslationKey normalizedKey = new TranslationKey(
                    pending.key().getEntityType(),
                    pending.key().getOriginalText(),
                    pending.key().getLanguage());
            TranslationData existing = findExisting(normalizedKey);

            TranslationEntry entry = TranslationEntry.register(command, toState(existing));
            if (existing == null) {
                translationRepository.save(toData(entry.state()));
            }
        } catch (DuplicateKeyException exception) {
            log.warn("并发登记赛事目录缺译键冲突，按已存在处理: entityType={}, originalText={}, language={}",
                    key.getEntityType(), key.getOriginalText(), key.getLanguage());
        } catch (Exception exception) {
            log.error("登记赛事目录缺译键失败，继续处理后续项: entityType={}, originalText={}, language={}",
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
