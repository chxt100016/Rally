package com.rally.tour;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.repository.MatchQueryRepository;
import com.rally.domain.tour.repository.TourDrawRepository;
import com.rally.domain.tour.repository.TourEntryRepository;
import com.rally.domain.tour.repository.TourPlayerRepository;
import com.rally.domain.tour.repository.TourTournamentRepository;
import com.rally.domain.tour.model.*;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.utils.Assert;
import com.rally.tour.poster.PosterPromptBuilder;
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
    private TourTournamentQueryDomainService tourTournamentQueryDomainService;

    @Resource
    private TourTournamentRepository tourTournamentRepository;

    @Resource
    private MatchQueryRepository matchQueryRepository;

    @Resource
    private TourTranslationService tourTranslationService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 生成 3D 卡通风格的赛事海报生图提示词。
     */
    public String generatePosterPrompt(String tournamentId) {
        TournamentData data = tourTournamentQueryDomainService.findByTournamentId(tournamentId);
        Assert.notNull(data, BizErrorCode.TOURNAMENT_NOT_FOUND);

        return PosterPromptBuilder.build(data);
    }

    public String generateDailyContent() {
        LocalDate date = LocalDate.now();
        TranslationLanguageEnum lang = TranslationLanguageEnum.ZH_CN;

        List<TournamentGroupData> groups = tourTournamentQueryDomainService.findValidCurrentTournamentGroups(date);
        if (CollectionUtils.isEmpty(groups)) {
            return "暂无比赛";
        }

        List<DailyGroup> dailyGroups = groups.stream()
                .map(group -> new DailyGroup(group, tourMatchQueryDomainService.upcomingDateGroups(group.getTournamentIds())))
                .filter(item -> CollectionUtils.isNotEmpty(item.dateGroups()))
                .toList();
        if (dailyGroups.isEmpty()) {
            return "暂无比赛";
        }

        List<TournamentData> displayedTournaments = findDisplayedTournaments(dailyGroups);
        if (displayedTournaments.isEmpty()) {
            return "暂无比赛";
        }

        Map<String, String> translatedTournamentNames = translateTournamentNames(displayedTournaments, lang);
        StringBuilder md = new StringBuilder();
        md.append(joinTournamentNames(displayedTournaments, translatedTournamentNames)).append("\n");
        md.append(buildScheduleSummary(displayedTournaments, date)).append("\n\n");

        boolean hasScheduleSection = false;
        for (DailyGroup dailyGroup : dailyGroups) {
            for (MatchGroupDTO dateGroup : dailyGroup.dateGroups()) {
                if (CollectionUtils.isEmpty(dateGroup.getChildren())) continue;

                tourTranslationService.matchGroups(dateGroup.getChildren(), lang);
                for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
                    tourTranslationService.matches(courtGroup.getData(), lang);
                }

                List<TournamentData> dateTournaments = tournamentsForDate(dailyGroup.group(), dateGroup);
                if (dateTournaments.isEmpty()) continue;

                if (hasScheduleSection) {
                    md.append("\n");
                }
                hasScheduleSection = true;

                md.append(joinTournamentNames(dateTournaments, translatedTournamentNames)).append("赛程");
                String dateText = formatDateText(dateGroup.getKey());
                if (StringUtils.isNotBlank(dateText)) {
                    md.append(" | ").append(dateText);
                    String roundText = extractPrimaryRound(dateGroup.getChildren());
                    if (StringUtils.isNotBlank(roundText)) {
                        md.append(" ").append(roundText);
                    }
                }
                md.append("\n");

                for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
                    if (CollectionUtils.isEmpty(courtGroup.getData())) continue;
                    md.append(courtGroup.getName()).append("\n");
                    for (MatchQueryVO match : courtGroup.getData()) {
                        appendMatch(md, match);
                    }
                }
            }
        }

        return md.toString();
    }

    private List<TournamentData> findDisplayedTournaments(List<DailyGroup> dailyGroups) {
        Map<String, TournamentData> tournaments = new LinkedHashMap<>();
        for (DailyGroup dailyGroup : dailyGroups) {
            for (TournamentData tournament : dailyGroup.group().getTournaments()) {
                boolean hasMatches = dailyGroup.dateGroups().stream()
                        .flatMap(dateGroup -> CollectionUtils.isEmpty(dateGroup.getChildren())
                                ? java.util.stream.Stream.empty()
                                : dateGroup.getChildren().stream())
                        .flatMap(courtGroup -> CollectionUtils.isEmpty(courtGroup.getData())
                                ? java.util.stream.Stream.empty()
                                : courtGroup.getData().stream())
                        .anyMatch(match -> Objects.equals(match.getTournamentId(), tournament.getTournamentId()));
                if (hasMatches) {
                    tournaments.putIfAbsent(tournament.getTournamentId(), tournament);
                }
            }
        }
        return new ArrayList<>(tournaments.values());
    }

    private Map<String, String> translateTournamentNames(List<TournamentData> tournaments, TranslationLanguageEnum lang) {
        Set<String> names = tournaments.stream()
                .map(TournamentData::getName)
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<TranslationKey> keys = names.stream()
                .map(name -> new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, name, lang))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<TranslationKey, String> translations = tourTranslationService.translate(keys, lang);

        Map<String, String> result = new LinkedHashMap<>();
        for (String name : names) {
            TranslationKey key = new TranslationKey(TranslationEntityTypeEnum.TOURNAMENT, name, lang);
            result.put(name, StringUtils.defaultIfBlank(translations.get(key), name));
        }
        return result;
    }

    private String joinTournamentNames(List<TournamentData> tournaments, Map<String, String> translatedTournamentNames) {
        return tournaments.stream()
                .map(TournamentData::getName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .map(name -> translatedTournamentNames.getOrDefault(name, name))
                .collect(Collectors.joining("｜"));
    }

    private String buildScheduleSummary(List<TournamentData> tournaments, LocalDate date) {
        Set<String> tours = tournaments.stream()
                .map(TournamentData::getTour)
                .filter(StringUtils::isNotBlank)
                .map(tour -> tour.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> summary = new ArrayList<>();
        if (tours.contains("ATP")) summary.add("ATP赛程");
        if (tours.contains("WTA")) summary.add("WTA赛程");
        summary.add(formatDateText(date.format(DATE_FMT)));
        return String.join("｜", summary);
    }

    private List<TournamentData> tournamentsForDate(TournamentGroupData group, MatchGroupDTO dateGroup) {
        Set<String> tournamentIds = dateGroup.getChildren().stream()
                .filter(Objects::nonNull)
                .flatMap(courtGroup -> CollectionUtils.isEmpty(courtGroup.getData())
                        ? java.util.stream.Stream.empty()
                        : courtGroup.getData().stream())
                .map(MatchQueryVO::getTournamentId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return group.getTournaments().stream()
                .filter(tournament -> tournamentIds.contains(tournament.getTournamentId()))
                .toList();
    }

    private void appendMatch(StringBuilder md, MatchQueryVO match) {
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

    public String generateSeedListContent(List<String> tournamentIds, TranslationLanguageEnum lang) {
        if (CollectionUtils.isEmpty(tournamentIds)) {
            return "# 种子名单\n\n无赛事信息";
        }

        List<TournamentData> tournaments = tourTournamentRepository.listByTournamentIds(tournamentIds);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "# 种子名单\n\n无赛事信息";
        }

        List<TournamentGroupData> groups = tourTournamentQueryDomainService.groupAndSortTournaments(tournaments);

        StringBuilder md = new StringBuilder();
        md.append("# 种子名单\n\n");

        for (TournamentGroupData group : groups) {
            List<SeedVO> seeds = tourMatchQueryDomainService.seeds(group.getTournamentIds());
            if (CollectionUtils.isEmpty(seeds)) continue;

            tourTranslationService.seeds(seeds, lang);

            md.append("## ").append(groupTitle(group.getTournaments())).append("\n\n");

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

    private String groupTitle(List<TournamentData> group) {
        return group.stream().map(t -> t.getName() + " (" + t.getTour() + ")").collect(Collectors.joining(" / "));
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

    private String extractPrimaryRound(List<MatchGroupDTO> courtGroups) {
        if (courtGroups == null) return "";
        for (MatchGroupDTO courtGroup : courtGroups) {
            if (CollectionUtils.isNotEmpty(courtGroup.getData())) {
                MatchQueryVO firstMatch = courtGroup.getData().get(0);
                if (StringUtils.isNotBlank(firstMatch.getRoundShow())) {
                    return firstMatch.getRoundShow();
                }
            }
        }
        return "";
    }

    private record DailyGroup(TournamentGroupData group, List<MatchGroupDTO> dateGroups) {
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
