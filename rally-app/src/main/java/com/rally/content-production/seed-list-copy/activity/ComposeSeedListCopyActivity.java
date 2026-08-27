package com.rally.contentproduction.seedlistcopy.activity;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.SeedStatusEnum;
import com.rally.domain.tour.model.SeedVO;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务活动 compose-seed-list-copy：只读生成所选职业赛事的种子名单文案及缺译键。
 */
@Component
@RequiredArgsConstructor
public class ComposeSeedListCopyActivity {

    private static final String COPY_TITLE = "# 种子名单\n\n";
    private static final String NO_TOURNAMENTS_COPY = COPY_TITLE + "无赛事信息";

    private final TourTournamentRepository tournamentRepository;
    private final TourTournamentQueryDomainService tournamentQueryService;
    private final TourMatchQueryDomainService matchQueryService;
    private final TranslationRepository translationRepository;

    public ComposeSeedListCopyResult execute(List<String> tournamentIds,
                                             TranslationLanguageEnum language) {
        // A1 忽略无效编号，并沿用既有同城、日期相交分组与稳定排序规则。
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return noTournaments();
        }
        List<TournamentData> tournaments = tournamentRepository.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(tournaments)) {
            return noTournaments();
        }
        List<TournamentGroupData> groups = tournamentQueryService.groupAndSortTournaments(tournaments);

        // A2/A3 由既有只读领域查询汇总非零种子、球员资料与 FINISHED 比赛并计算淘汰状态。
        List<SeedGroup> seedGroups = new ArrayList<>();
        for (TournamentGroupData group : groups) {
            List<SeedVO> seeds = matchQueryService.seeds(group.getTournamentIds());
            if (CollectionUtils.isNotEmpty(seeds)) {
                seedGroups.add(new SeedGroup(group, seeds));
            }
        }

        // A4 只读取 PLAYER 目标语言译文；未命中时保留原名并返回去重缺译键。
        Set<TranslationKey> requestedKeys = collectTranslationKeys(seedGroups, language);
        Map<TranslationKey, String> translations = findTranslations(requestedKeys);
        Set<TranslationKey> missingTranslations = requestedKeys.stream()
                .filter(key -> StringUtils.isBlank(translations.get(key)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        applyTranslations(seedGroups, translations, language);

        // A5 跳过空赛事组，按赛事组、巡回赛首次出现顺序与种子号升序编排 Markdown。
        String copy = buildCopy(seedGroups);
        return new ComposeSeedListCopyResult(copy, missingTranslations);
    }

    private ComposeSeedListCopyResult noTournaments() {
        return new ComposeSeedListCopyResult(NO_TOURNAMENTS_COPY, Set.of());
    }

    private Set<TranslationKey> collectTranslationKeys(List<SeedGroup> groups,
                                                       TranslationLanguageEnum language) {
        Set<TranslationKey> keys = new LinkedHashSet<>();
        for (SeedGroup group : groups) {
            for (SeedVO seed : group.seeds()) {
                if (StringUtils.isNotBlank(seed.getName())) {
                    keys.add(new TranslationKey(TranslationEntityTypeEnum.PLAYER, seed.getName(), language));
                }
            }
        }
        return keys;
    }

    private Map<TranslationKey, String> findTranslations(Set<TranslationKey> keys) {
        if (keys.isEmpty()) {
            return Map.of();
        }
        List<TranslationData> queries = keys.stream()
                .map(key -> new TranslationData()
                        .setEntityType(key.getEntityType())
                        .setOriginalText(key.getOriginalText())
                        .setLanguage(key.getLanguage()))
                .toList();
        List<TranslationData> existing = translationRepository.findBatch(queries);
        if (CollectionUtils.isEmpty(existing)) {
            return Map.of();
        }
        Map<TranslationKey, String> result = new LinkedHashMap<>();
        for (TranslationData translation : existing) {
            if (translation == null || translation.getEntityType() != TranslationEntityTypeEnum.PLAYER
                    || StringUtils.isBlank(translation.getOriginalText())
                    || StringUtils.isBlank(translation.getTranslatedText())) {
                continue;
            }
            TranslationKey key = new TranslationKey(
                    translation.getEntityType(), translation.getOriginalText(), translation.getLanguage());
            result.put(key, translation.getTranslatedText());
        }
        return result;
    }

    private void applyTranslations(List<SeedGroup> groups,
                                   Map<TranslationKey, String> translations,
                                   TranslationLanguageEnum language) {
        for (SeedGroup group : groups) {
            for (SeedVO seed : group.seeds()) {
                if (StringUtils.isBlank(seed.getName())) {
                    continue;
                }
                String translated = translations.get(new TranslationKey(
                        TranslationEntityTypeEnum.PLAYER, seed.getName(), language));
                if (StringUtils.isNotBlank(translated)) {
                    seed.setName(translated);
                }
            }
        }
    }

    private String buildCopy(List<SeedGroup> groups) {
        StringBuilder copy = new StringBuilder(COPY_TITLE);
        for (SeedGroup group : groups) {
            copy.append("## ").append(groupTitle(group.tournamentGroup().getTournaments())).append("\n\n");

            Map<String, List<SeedVO>> seedsByTour = group.seeds().stream()
                    .collect(Collectors.groupingBy(
                            SeedVO::getTour, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<SeedVO>> entry : seedsByTour.entrySet()) {
                if (seedsByTour.size() > 1) {
                    copy.append("### ").append(entry.getKey()).append("\n\n");
                }
                appendSeedTable(copy, entry.getValue());
            }
        }
        return copy.toString();
    }

    private String groupTitle(List<TournamentData> tournaments) {
        return tournaments.stream()
                .map(tournament -> tournament.getName() + " (" + tournament.getTour() + ")")
                .collect(Collectors.joining(" / "));
    }

    private void appendSeedTable(StringBuilder copy, List<SeedVO> seeds) {
        copy.append("| 种子 | 球员 | 国家/地区 | 状态 |\n");
        copy.append("|------|------|----------|------|\n");
        seeds.stream()
                .sorted(Comparator.comparing(SeedVO::getSeed,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(seed -> copy.append("| ")
                        .append(seed.getSeed())
                        .append(" | ")
                        .append(StringUtils.defaultString(seed.getName()))
                        .append(" | ")
                        .append(countryCode(seed))
                        .append(" | ")
                        .append(statusText(seed))
                        .append(" |\n"));
        copy.append("\n");
    }

    private String countryCode(SeedVO seed) {
        return seed.getCountry() != null && StringUtils.isNotBlank(seed.getCountry().getCode())
                ? seed.getCountry().getCode()
                : "";
    }

    private String statusText(SeedVO seed) {
        if (seed.getStatus() == SeedStatusEnum.ELIMINATED) {
            return "已淘汰" + (StringUtils.isNotBlank(seed.getLabel()) ? " (" + seed.getLabel() + ")" : "");
        }
        return seed.getStatus() == SeedStatusEnum.ACTIVE ? "参赛中" : "";
    }

    private record SeedGroup(TournamentGroupData tournamentGroup, List<SeedVO> seeds) {
    }
}
