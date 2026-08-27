package com.rally.platformconfig.homecontentquery.activity;

import com.alibaba.fastjson2.JSONObject;
import com.rally.domain.media.assetstorage.AssetStorageGateway;
import com.rally.domain.media.assetstorage.AssetStorageService;
import com.rally.domain.media.assetstorage.SignedReadOutcome;
import com.rally.domain.media.assetstorage.SignedReadResult;
import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.MatchGroupDTO;
import com.rally.domain.tour.model.MatchQueryVO;
import com.rally.domain.tour.model.PlayerVO;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import com.rally.domain.translation.cache.TranslationCache;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.home.convert.HomeAppConvertMapper;
import com.rally.home.model.DisplayType;
import com.rally.home.model.HomeDisplayItemDTO;
import com.rally.home.model.MatchDisplayData;
import com.rally.home.model.TournamentDisplayDTO;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 业务活动 query-home-tour-section：组装首页职业赛事及最近比赛区块。 */
@Component
@RequiredArgsConstructor
public class QueryHomeTourSectionActivity {

    private static final long IMAGE_URL_TTL_SECONDS = 3600L;
    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TourTournamentQueryDomainService tournamentQueryService;
    private final TourMatchQueryDomainService matchQueryService;
    private final TranslationCache translationCache;
    private final AssetStorageGateway assetStorageGateway;

    public QueryHomeTourSectionResult execute(JSONObject section) {
        return execute(section, LocalDate.now());
    }

    public QueryHomeTourSectionResult execute(JSONObject section, LocalDate currentDate) {
        // A1：领域查询保留 main 的前后一天窗口、类别过滤、跨巡回赛合并和稳定排序。
        List<TournamentGroupData> tournamentGroups =
                tournamentQueryService.findValidCurrentTournamentGroups(currentDate);
        if (CollectionUtils.isEmpty(tournamentGroups)) {
            return QueryHomeTourSectionResult.empty();
        }

        List<TournamentDisplayDTO> tournamentDisplays = new ArrayList<>();
        List<TournamentGroupData> displayedTournamentGroups = new ArrayList<>();
        for (TournamentGroupData tournamentGroup : tournamentGroups) {
            // A2/A3：每组只展示最早日期、排序首个球场的全部比赛；已知缺失条件仅跳过该组。
            TournamentDisplayDTO display = buildTournamentDisplay(tournamentGroup);
            if (display != null) {
                tournamentDisplays.add(display);
                displayedTournamentGroups.add(tournamentGroup);
            }
        }
        if (tournamentDisplays.isEmpty()) {
            return QueryHomeTourSectionResult.empty();
        }

        // A4：读取简中翻译，空译文或未命中项保留原文并交给后续登记活动。
        TranslationResult translationResult = translate(tournamentDisplays);

        MatchDisplayData data = new MatchDisplayData();
        data.setTitle(configuredText(section, "title", "巡回赛"));
        data.setSubtitle(configuredText(
                section, "subtitle", buildTourSubtitle(displayedTournamentGroups)));
        data.setTournaments(tournamentDisplays);

        HomeDisplayItemDTO item = new HomeDisplayItemDTO();
        item.setDisplayType(DisplayType.TOUR_MATCH);
        item.setData(data);
        return new QueryHomeTourSectionResult(item, translationResult.missingKeys());
    }

    private TournamentDisplayDTO buildTournamentDisplay(TournamentGroupData tournamentGroup) {
        List<String> tournamentIds = tournamentGroup.getTournamentIds();
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return null;
        }

        List<MatchGroupDTO> dateGroups = matchQueryService.upcomingDateGroups(tournamentIds);
        if (CollectionUtils.isEmpty(dateGroups)) {
            return null;
        }

        MatchGroupDTO firstDateGroup = dateGroups.get(0);
        if (CollectionUtils.isEmpty(firstDateGroup.getChildren())) {
            return null;
        }

        MatchGroupDTO firstCourtGroup = firstDateGroup.getChildren().get(0);
        if (CollectionUtils.isEmpty(firstCourtGroup.getData())) {
            return null;
        }

        TournamentData representative = tournamentGroup.getRepresentative();
        TournamentDisplayDTO dto = HomeAppConvertMapper.INSTANCE
                .toTournamentDisplayDTO(representative);
        dto.setTour(joinTours(tournamentGroup.getTournaments()));
        dto.setCourtName(firstCourtGroup.getName());
        dto.setMatchDate(LocalDate.parse(firstDateGroup.getKey()));
        dto.setImagePath(signImage(representative.getImagePath()));
        dto.setMatches(firstCourtGroup.getData());
        return dto;
    }

    private String signImage(String imageKey) {
        if (StringUtils.isBlank(imageKey)) {
            return null;
        }
        SignedReadResult result = new AssetStorageService(assetStorageGateway)
                .signReadUrl(imageKey, IMAGE_URL_TTL_SECONDS);
        if (result.getOutcome() != SignedReadOutcome.SIGNED) {
            throw new IllegalStateException("签发首页赛事图片地址失败");
        }
        return result.getSignedUrl();
    }

    private TranslationResult translate(List<TournamentDisplayDTO> displays) {
        Set<TranslationKey> requestedKeys = collectTranslationKeys(displays);
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

        for (TournamentDisplayDTO display : displays) {
            display.setTournamentName(translated(
                    TranslationEntityTypeEnum.TOURNAMENT,
                    display.getTournamentName(),
                    translations));
            display.setCourtName(translated(
                    TranslationEntityTypeEnum.COURT,
                    display.getCourtName(),
                    translations));
            if (CollectionUtils.isEmpty(display.getMatches())) {
                continue;
            }
            for (MatchQueryVO match : display.getMatches()) {
                match.setCourt(translated(
                        TranslationEntityTypeEnum.COURT, match.getCourt(), translations));
                translatePlayer(match.getPlayer1(), translations);
                translatePlayer(match.getPlayer2(), translations);
            }
        }
        return new TranslationResult(missingKeys);
    }

    private Set<TranslationKey> collectTranslationKeys(List<TournamentDisplayDTO> displays) {
        Set<TranslationKey> keys = new LinkedHashSet<>();
        for (TournamentDisplayDTO display : displays) {
            addTranslationKey(
                    keys, TranslationEntityTypeEnum.TOURNAMENT, display.getTournamentName());
            addTranslationKey(keys, TranslationEntityTypeEnum.COURT, display.getCourtName());
            if (CollectionUtils.isEmpty(display.getMatches())) {
                continue;
            }
            for (MatchQueryVO match : display.getMatches()) {
                addTranslationKey(keys, TranslationEntityTypeEnum.COURT, match.getCourt());
                addPlayerTranslationKey(keys, match.getPlayer1());
                addPlayerTranslationKey(keys, match.getPlayer2());
            }
        }
        return keys;
    }

    private void addPlayerTranslationKey(Set<TranslationKey> keys, PlayerVO player) {
        if (player != null) {
            addTranslationKey(keys, TranslationEntityTypeEnum.PLAYER, player.getName());
        }
    }

    private void addTranslationKey(Set<TranslationKey> keys,
                                   TranslationEntityTypeEnum entityType,
                                   String originalText) {
        if (StringUtils.isNotBlank(originalText)) {
            keys.add(new TranslationKey(entityType, originalText, TARGET_LANGUAGE));
        }
    }

    private void translatePlayer(PlayerVO player, Map<TranslationKey, String> translations) {
        if (player != null) {
            player.setName(translated(
                    TranslationEntityTypeEnum.PLAYER, player.getName(), translations));
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

    private String buildTourSubtitle(List<TournamentGroupData> tournamentGroups) {
        Map<String, Long> tourCountMap = tournamentGroups.stream()
                .flatMap(group -> group.getTournaments().stream())
                .map(TournamentData::getTour)
                .filter(StringUtils::isNotBlank)
                .map(tour -> tour.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.groupingBy(tour -> tour, Collectors.counting()));
        long atpCount = tourCountMap.getOrDefault("ATP", 0L);
        long wtaCount = tourCountMap.getOrDefault("WTA", 0L);

        StringBuilder subtitle = new StringBuilder();
        if (atpCount > 0) {
            subtitle.append(atpCount).append("场ATP、");
        }
        if (wtaCount > 0) {
            subtitle.append(wtaCount).append("场WTA、");
        }
        if (!subtitle.isEmpty()) {
            subtitle.setLength(subtitle.length() - 1);
        }
        return subtitle.append("进行中").toString();
    }

    private String joinTours(List<TournamentData> tournaments) {
        return tournaments.stream()
                .map(TournamentData::getTour)
                .filter(StringUtils::isNotBlank)
                .map(tour -> tour.trim().toUpperCase(Locale.ROOT))
                .distinct()
                .sorted((first, second) -> {
                    int order = Integer.compare(tourOrder(first), tourOrder(second));
                    return order != 0 ? order : first.compareTo(second);
                })
                .collect(Collectors.joining("/"));
    }

    private int tourOrder(String tour) {
        return switch (tour) {
            case "ATP" -> 0;
            case "WTA" -> 1;
            default -> 2;
        };
    }

    private String configuredText(JSONObject section, String key, String fallback) {
        String value = section.getString(key);
        return StringUtils.isBlank(value) ? fallback : value;
    }

    private record TranslationResult(Set<TranslationKey> missingKeys) {
    }
}
