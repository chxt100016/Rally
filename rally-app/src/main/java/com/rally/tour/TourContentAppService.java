package com.rally.tour;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.repository.MatchQueryRepository;
import com.rally.domain.tour.repository.TourDrawRepository;
import com.rally.domain.tour.repository.TourEntryRepository;
import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.tour.model.*;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.translation.TourTranslationService;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TourContentAppService {

    @Resource
    private TourMatchQueryDomainService tourMatchQueryDomainService;

    @Resource
    private TourTournamentRepository tourTournamentRepository;

    @Resource
    private MatchQueryRepository matchQueryRepository;

    @Resource
    private TourTranslationService tourTranslationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public String generateDailyContent() {
        LocalDate date = LocalDate.now();
        TranslationLanguageEnum lang = TranslationLanguageEnum.ZH_CN;

        List<TournamentData> tournaments = tourTournamentRepository.findCurrentTournaments(date);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "# 比赛日程\n\n暂无比赛";
        }

        List<List<TournamentData>> groups = groupByOverlap(tournaments);
        StringBuilder md = new StringBuilder();
        md.append("# 比赛日程\n\n");

        for (List<TournamentData> group : groups) {
            List<String> groupIds = group.stream().map(TournamentData::getTournamentId).toList();
            List<MatchGroupDTO> dateGroups = tourMatchQueryDomainService.upcomingDateGroups(groupIds);
            if (CollectionUtils.isEmpty(dateGroups)) continue;

            String translatedTournamentName = translateAndJoinTournamentNames(group, lang);

            for (MatchGroupDTO dateGroup : dateGroups) {
                String dateText = formatDateText(dateGroup.getKey());
                String roundText = extractPrimaryRound(dateGroup);

                md.append("## ").append(translatedTournamentName).append("赛程");
                if (StringUtils.isNotBlank(dateText)) {
                    md.append(" | ").append(dateText);
                    if (StringUtils.isNotBlank(roundText)) {
                        md.append(" ").append(roundText);
                    }
                }
                md.append("\n\n");

                if (CollectionUtils.isNotEmpty(dateGroup.getChildren())) {
                    tourTranslationService.matchGroups(dateGroup.getChildren(), lang);
                    for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
                        if (CollectionUtils.isNotEmpty(courtGroup.getData())) {
                            tourTranslationService.matches(courtGroup.getData(), lang);
                            md.append(courtGroup.getName()).append("\n");
                            for (MatchQueryVO match : courtGroup.getData()) {
                                md.append(formatPlayerName(match.getPlayer1())).append(" vs ").append(formatPlayerName(match.getPlayer2()));
                                if (StringUtils.isNotBlank(match.getScheduledShow())) {
                                    md.append(" | ").append(match.getScheduledShow());
                                }
                                if (CollectionUtils.isNotEmpty(match.getSets())) {
                                    md.append(" | ");
                                    for (SetScoreVO set : match.getSets()) {
                                        md.append(set.getPlayer1()).append("-").append(set.getPlayer2()).append(" ");
                                    }
                                }
                                if (StringUtils.isNotBlank(match.getStatusLabel())) {
                                    md.append(" | ").append(match.getStatusLabel());
                                }
                                md.append("\n");
                            }
                            md.append("\n");
                        }
                    }
                }
            }
        }

        return md.toString();
    }

    public String generateSeedListContent(List<String> tournamentIds, TranslationLanguageEnum lang) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return "# 种子名单\n\n无赛事信息";
        }

        List<TournamentData> tournaments = tourTournamentRepository.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "# 种子名单\n\n无赛事信息";
        }

        List<List<TournamentData>> groups = groupByOverlap(tournaments);

        StringBuilder md = new StringBuilder();
        md.append("# 种子名单\n\n");

        for (List<TournamentData> group : groups) {
            List<String> groupIds = group.stream().map(TournamentData::getTournamentId).toList();
            List<SeedVO> seeds = tourMatchQueryDomainService.seeds(groupIds);
            if (CollectionUtils.isEmpty(seeds)) continue;

            tourTranslationService.seeds(seeds, lang);

            md.append("## ").append(groupTitle(group)).append("\n\n");

            Map<String, List<SeedVO>> bySeed = seeds.stream().collect(Collectors.groupingBy(SeedVO::getTour, LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<String, List<SeedVO>> entry : bySeed.entrySet()) {
                if (bySeed.size() > 1) {
                    md.append("### ").append(entry.getKey()).append("\n\n");
                }
                List<SeedVO> sortedSeeds = entry.getValue().stream().sorted(Comparator.comparing(SeedVO::getSeed, Comparator.nullsLast(Comparator.naturalOrder()))).toList();
                md.append("| 种子 | 球员 | 国家/地区 | 状态 |\n");
                md.append("|------|------|----------|------|\n");
                for (SeedVO seed : sortedSeeds) {
                    String country = seed.getCountry() != null && StringUtils.isNotBlank(seed.getCountry().getCode()) ? seed.getCountry().getCode() : "";
                    String status = "";
                    if (seed.getStatus() == SeedStatusEnum.ELIMINATED) {
                        status = "已淘汰" + (StringUtils.isNotBlank(seed.getLabel()) ? " (" + seed.getLabel() + ")" : "");
                    } else if (seed.getStatus() == SeedStatusEnum.ACTIVE) {
                        status = "参赛中";
                    }
                    md.append("| ").append(seed.getSeed()).append(" | ").append(seed.getName()).append(" | ").append(country).append(" | ").append(status).append(" |\n");
                }
                md.append("\n");
            }
        }

        return md.toString();
    }

    /** 将赛事按"日期重叠 + 同城（忽略大小写）"分组，用 union-find 保证传递性合并 */
    private List<List<TournamentData>> groupByOverlap(List<TournamentData> tournaments) {
        int n = tournaments.size();
        int[] parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                if (shouldMerge(tournaments.get(i), tournaments.get(j))) union(parent, i, j);
            }
        }
        Map<Integer, List<TournamentData>> groups = new LinkedHashMap<>();
        for (int i = 0; i < n; i++) {
            groups.computeIfAbsent(find(parent, i), k -> new ArrayList<>()).add(tournaments.get(i));
        }
        return new ArrayList<>(groups.values());
    }

    private boolean shouldMerge(TournamentData a, TournamentData b) {
        if (a.getStartDate() == null || a.getEndDate() == null || b.getStartDate() == null || b.getEndDate() == null) return false;
        if (StringUtils.isBlank(a.getCity()) || StringUtils.isBlank(b.getCity())) return false;
        boolean overlap = !a.getEndDate().isBefore(b.getStartDate()) && !b.getEndDate().isBefore(a.getStartDate());
        return overlap && a.getCity().equalsIgnoreCase(b.getCity());
    }

    private int find(int[] parent, int i) {
        while (parent[i] != i) { parent[i] = parent[parent[i]]; i = parent[i]; }
        return i;
    }

    private void union(int[] parent, int i, int j) {
        parent[find(parent, i)] = find(parent, j);
    }

    private String groupTitle(List<TournamentData> group) {
        return group.stream().map(t -> t.getName() + " (" + t.getTour() + ")").collect(Collectors.joining(" / "));
    }

    private String translateAndJoinTournamentNames(List<TournamentData> group, TranslationLanguageEnum lang) {
        if (CollectionUtils.isEmpty(group)) return "";
        TournamentData first = group.get(0);
        TranslationKey key = new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, first.getName(), lang);
        Map<TranslationKey, String> translations = tourTranslationService.translate(Set.of(key), lang);
        String name = translations.getOrDefault(key, first.getName());
        String tours = group.stream().map(TournamentData::getTour).filter(StringUtils::isNotBlank).distinct().collect(Collectors.joining("/"));
        return StringUtils.isNotBlank(tours) ? name + " | " + tours : name;
    }

    private String formatDateText(String dateKey) {
        if (StringUtils.isBlank(dateKey)) return "";
        try {
            LocalDate date = LocalDate.parse(dateKey, DATE_FMT);
            return date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
        } catch (Exception e) {
            return dateKey;
        }
    }

    private String extractPrimaryRound(MatchGroupDTO dateGroup) {
        if (dateGroup.getChildren() == null) return "";
        for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
            if (CollectionUtils.isNotEmpty(courtGroup.getData())) {
                MatchQueryVO firstMatch = courtGroup.getData().get(0);
                if (StringUtils.isNotBlank(firstMatch.getRoundShow())) {
                    return firstMatch.getRoundShow();
                }
            }
        }
        return "";
    }

    private String formatPlayerName(PlayerVO player) {
        if (player == null) {
            return "待定";
        }

        StringBuilder sb = new StringBuilder();

        if (player.getCountry() != null && StringUtils.isNotBlank(player.getCountry().getFlagCode())) {
            sb.append(countryFlag(player.getCountry().getFlagCode())).append(" ");
        }

        sb.append(player.getName());

        if (player.getSeed() != null) {
            sb.append("[").append(player.getSeed()).append("]");
        }

        return sb.toString();
    }

    private String countryFlag(String iso2Code) {
        if (StringUtils.isBlank(iso2Code) || iso2Code.length() != 2) {
            return "";
        }
        String code = iso2Code.toUpperCase();
        int first = Character.codePointAt(code, 0) - 'A' + 0x1F1E6;
        int second = Character.codePointAt(code, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }
}
