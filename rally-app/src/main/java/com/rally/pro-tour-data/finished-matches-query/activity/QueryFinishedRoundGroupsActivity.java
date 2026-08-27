package com.rally.protourdata.finishedmatchesquery.activity;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.model.MatchGroupDTO;
import com.rally.domain.tour.model.MatchQueryVO;
import com.rally.domain.tour.model.PlayerVO;
import com.rally.domain.tour.model.SeedGroupDTO;
import com.rally.domain.tour.model.SeedVO;
import com.rally.domain.tour.model.TourMatchDTO;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 业务活动 query-finished-round-groups：组装完赛轮次分组并应用已有简中译文。
 */
@Component
@RequiredArgsConstructor
public class QueryFinishedRoundGroupsActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TourMatchQueryDomainService tourMatchQueryDomainService;
    private final TranslationCache translationCache;

    public Result execute(List<String> tournamentIds, List<SeedGroupDTO> seedGroups) {
        // A1/A2/A3：复用 main 的类型化查询，保留 FINISHED 过滤、startedAt 倒序、
        // 球员/种子/国家/比分映射，以及已知轮次优先、未知轮次按首次出现的分组顺序。
        List<MatchGroupDTO> roundGroups = tourMatchQueryDomainService.finishedRoundGroups(tournamentIds);

        TourMatchDTO dto = new TourMatchDTO();
        dto.setSeed(seedGroups == null ? List.of() : seedGroups);
        dto.setMatch(roundGroups == null ? List.of() : roundGroups);

        // A4：一次收集上游种子和本活动比赛的姓名/球场键。空译文与未命中
        // 都保留原文并输出给后续登记活动，本活动不产生写入。
        Set<TranslationKey> requestedKeys = collectTranslationKeys(dto);
        Map<TranslationKey, String> translations = new LinkedHashMap<>();
        Set<TranslationKey> missingKeys = new LinkedHashSet<>();
        for (TranslationKey key : requestedKeys) {
            String translated = translationCache.get(key);
            if (StringUtils.isNotBlank(translated)) {
                translations.put(key, translated);
            } else {
                missingKeys.add(key);
            }
        }
        applyTranslations(dto, translations);
        return new Result(dto, missingKeys);
    }

    private Set<TranslationKey> collectTranslationKeys(TourMatchDTO dto) {
        Set<TranslationKey> keys = new LinkedHashSet<>();
        for (SeedGroupDTO group : dto.getSeed()) {
            if (group == null || group.getData() == null) {
                continue;
            }
            for (SeedVO seed : group.getData()) {
                if (seed != null) {
                    addKey(keys, TranslationEntityTypeEnum.PLAYER, seed.getName());
                }
            }
        }
        for (MatchGroupDTO group : dto.getMatch()) {
            if (group == null || group.getData() == null) {
                continue;
            }
            for (MatchQueryVO match : group.getData()) {
                if (match == null) {
                    continue;
                }
                addKey(keys, TranslationEntityTypeEnum.COURT, match.getCourt());
                addPlayerKey(keys, match.getPlayer1());
                addPlayerKey(keys, match.getPlayer2());
            }
        }
        return keys;
    }

    private void addPlayerKey(Set<TranslationKey> keys, PlayerVO player) {
        if (player != null) {
            addKey(keys, TranslationEntityTypeEnum.PLAYER, player.getName());
        }
    }

    private void addKey(Set<TranslationKey> keys,
                        TranslationEntityTypeEnum entityType,
                        String originalText) {
        if (StringUtils.isNotBlank(originalText)) {
            keys.add(new TranslationKey(entityType, originalText, TARGET_LANGUAGE));
        }
    }

    private void applyTranslations(TourMatchDTO dto, Map<TranslationKey, String> translations) {
        for (SeedGroupDTO group : dto.getSeed()) {
            if (group == null || group.getData() == null) {
                continue;
            }
            for (SeedVO seed : group.getData()) {
                if (seed != null) {
                    seed.setName(translated(TranslationEntityTypeEnum.PLAYER, seed.getName(), translations));
                }
            }
        }
        for (MatchGroupDTO group : dto.getMatch()) {
            if (group == null || group.getData() == null) {
                continue;
            }
            for (MatchQueryVO match : group.getData()) {
                if (match == null) {
                    continue;
                }
                match.setCourt(translated(TranslationEntityTypeEnum.COURT, match.getCourt(), translations));
                translatePlayer(match.getPlayer1(), translations);
                translatePlayer(match.getPlayer2(), translations);
            }
        }
    }

    private void translatePlayer(PlayerVO player, Map<TranslationKey, String> translations) {
        if (player != null) {
            player.setName(translated(TranslationEntityTypeEnum.PLAYER, player.getName(), translations));
        }
    }

    private String translated(TranslationEntityTypeEnum entityType,
                              String originalText,
                              Map<TranslationKey, String> translations) {
        if (StringUtils.isBlank(originalText)) {
            return originalText;
        }
        return StringUtils.defaultIfBlank(
                translations.get(new TranslationKey(entityType, originalText, TARGET_LANGUAGE)),
                originalText);
    }

    /** 保留上游种子的完整响应，以及交给后续活动的去重缺译键。 */
    public record Result(TourMatchDTO data, Set<TranslationKey> missingTranslationKeys) {
        public Result {
            if (data == null) {
                data = new TourMatchDTO();
                data.setSeed(List.of());
                data.setMatch(List.of());
            }
            missingTranslationKeys = missingTranslationKeys == null
                    ? Set.of()
                    : Collections.unmodifiableSet(new LinkedHashSet<>(missingTranslationKeys));
        }
    }
}
