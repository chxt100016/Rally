package com.rally.protourdata.upcomingmatchesquery.activity;

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
import java.util.Objects;
import java.util.Set;

/**
 * 业务活动 query-upcoming-match-groups：组装种子状态与日期、球场两级待赛分组。
 */
@Component
@RequiredArgsConstructor
public class QueryUpcomingMatchGroupsActivity {

    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TourMatchQueryDomainService tourMatchQueryDomainService;
    private final TranslationCache translationCache;

    public Result execute(List<String> tournamentIds) {
        // A1-A4：沿用 main 的查询与组装逻辑，保留跨年份/签表种子汇总、
        // 最后遍历败局的淘汰标签、待赛日期同日完赛补入及全部 DTO 降级/排序语义。
        TourMatchDTO dto = new TourMatchDTO();
        List<SeedGroupDTO> seedGroups = tourMatchQueryDomainService.seedGroups(tournamentIds);
        List<MatchGroupDTO> matchGroups = tourMatchQueryDomainService.upcomingDateGroups(tournamentIds);
        dto.setSeed(seedGroups == null ? List.of() : seedGroups);
        dto.setMatch(matchGroups == null ? List.of() : matchGroups);

        // A5：一次去重收集球场与球员简中键，空译文或未命中均保留原文并输出缺口。
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
            collectMatchGroupKeys(group, keys);
        }
        return keys;
    }

    private void collectMatchGroupKeys(MatchGroupDTO group, Set<TranslationKey> keys) {
        if (group == null) {
            return;
        }
        if (isCourtGroup(group)) {
            addKey(keys, TranslationEntityTypeEnum.COURT, group.getName());
        }
        if (group.getData() != null) {
            for (MatchQueryVO match : group.getData()) {
                if (match == null) {
                    continue;
                }
                addKey(keys, TranslationEntityTypeEnum.COURT, match.getCourt());
                addPlayerKey(keys, match.getPlayer1());
                addPlayerKey(keys, match.getPlayer2());
            }
        }
        if (group.getChildren() != null) {
            for (MatchGroupDTO child : group.getChildren()) {
                collectMatchGroupKeys(child, keys);
            }
        }
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
            applyMatchGroupTranslations(group, translations);
        }
    }

    private void applyMatchGroupTranslations(MatchGroupDTO group,
                                             Map<TranslationKey, String> translations) {
        if (group == null) {
            return;
        }
        if (isCourtGroup(group)) {
            group.setName(translated(TranslationEntityTypeEnum.COURT, group.getName(), translations));
        }
        if (group.getData() != null) {
            for (MatchQueryVO match : group.getData()) {
                if (match == null) {
                    continue;
                }
                match.setCourt(translated(TranslationEntityTypeEnum.COURT, match.getCourt(), translations));
                translatePlayer(match.getPlayer1(), translations);
                translatePlayer(match.getPlayer2(), translations);
            }
        }
        if (group.getChildren() != null) {
            for (MatchGroupDTO child : group.getChildren()) {
                applyMatchGroupTranslations(child, translations);
            }
        }
    }

    private boolean isCourtGroup(MatchGroupDTO group) {
        if (group.getKey() == null || group.getData() == null || group.getData().isEmpty()) {
            return false;
        }
        return group.getData().stream()
                .filter(Objects::nonNull)
                .anyMatch(match -> Objects.equals(group.getKey(), match.getCourt()));
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

    /** 已应用现有译文的响应，以及交给后续登记活动的去重缺译键。 */
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
