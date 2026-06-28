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

    public String generateDailyContent(LocalDate date, TranslationLanguageEnum lang) {
        List<TournamentData> tournaments = tourTournamentRepository.findCurrentTournaments(date);
        if (CollectionUtils.isEmpty(tournaments)) {
            return "# " + date.format(DATE_FMT) + "\n\n暂无比赛";
        }

        List<String> tournamentIds = tournaments.stream().map(TournamentData::getTournamentId).toList();
        List<MatchData> matchDataList = matchQueryRepository.findByTournamentIdsAndDate(tournamentIds, date);
        if (CollectionUtils.isEmpty(matchDataList)) {
            return "# " + date.format(DATE_FMT) + "\n\n暂无比赛";
        }

        List<MatchQueryVO> matches = tourMatchQueryDomainService.upcomingMatches(tournamentIds);
        matches = matches.stream().filter(m -> date.format(DATE_FMT).equals(m.getDate())).toList();

        tourTranslationService.matches(matches, lang);

        Map<String, TournamentData> tournamentMap = tournaments.stream().collect(Collectors.toMap(TournamentData::getTournamentId, t -> t, (a, b) -> a));

        StringBuilder md = new StringBuilder();
        md.append("# ").append(date.format(DATE_FMT)).append(" 比赛日程\n\n");

        Map<String, List<MatchQueryVO>> tournamentMatchMap = matches.stream().collect(Collectors.groupingBy(MatchQueryVO::getTournamentId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<MatchQueryVO>> entry : tournamentMatchMap.entrySet()) {
            TournamentData tournament = tournamentMap.get(entry.getKey());
            if (tournament != null) {
                md.append("## ").append(tournament.getName()).append(" (").append(tournament.getTour()).append(")\n\n");
            }

            Map<String, List<MatchQueryVO>> courtMatchMap = entry.getValue().stream().collect(Collectors.groupingBy(m -> StringUtils.isNotBlank(m.getCourt()) ? m.getCourt() : "未知场地", LinkedHashMap::new, Collectors.toList()));

            for (Map.Entry<String, List<MatchQueryVO>> courtEntry : courtMatchMap.entrySet()) {
                md.append("### ").append(courtEntry.getKey()).append("\n\n");

                for (MatchQueryVO match : courtEntry.getValue()) {
                    md.append("- **").append(match.getRoundShow()).append("**");
                    if (StringUtils.isNotBlank(match.getScheduledShow())) {
                        md.append(" | ").append(match.getScheduledShow());
                    }
                    md.append("\n");

                    String player1 = formatPlayerName(match.getPlayer1());
                    String player2 = formatPlayerName(match.getPlayer2());
                    md.append("  - ").append(player1).append(" vs ").append(player2);

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

        List<SeedVO> seeds = tourMatchQueryDomainService.seeds(tournamentIds);
        if (CollectionUtils.isEmpty(seeds)) {
            return "# 种子名单\n\n暂无种子球员";
        }

        tourTranslationService.seeds(seeds, lang);

        Map<String, TournamentData> tournamentMap = tournaments.stream().collect(Collectors.toMap(TournamentData::getTournamentId, t -> t, (a, b) -> a));

        StringBuilder md = new StringBuilder();
        md.append("# 种子名单\n\n");

        Map<String, List<SeedVO>> tournamentSeedMap = seeds.stream().collect(Collectors.groupingBy(SeedVO::getTournamentId, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<SeedVO>> entry : tournamentSeedMap.entrySet()) {
            TournamentData tournament = tournamentMap.get(entry.getKey());
            if (tournament != null) {
                md.append("## ").append(tournament.getName()).append(" (").append(tournament.getTour()).append(")\n\n");
            }

            List<SeedVO> sortedSeeds = entry.getValue().stream().sorted(Comparator.comparing(SeedVO::getSeed, Comparator.nullsLast(Comparator.naturalOrder()))).toList();

            md.append("| 种子 | 球员 | 国家/地区 | 状态 |\n");
            md.append("|------|------|----------|------|\n");

            for (SeedVO seed : sortedSeeds) {
                md.append("| ").append(seed.getSeed()).append(" | ");
                md.append(seed.getName()).append(" | ");

                String country = "";
                if (seed.getCountry() != null && StringUtils.isNotBlank(seed.getCountry().getCode())) {
                    country = seed.getCountry().getCode();
                }
                md.append(country).append(" | ");

                String status = "";
                if (seed.getStatus() == SeedStatusEnum.ELIMINATED) {
                    status = "已淘汰";
                    if (StringUtils.isNotBlank(seed.getLabel())) {
                        status += " (" + seed.getLabel() + ")";
                    }
                } else if (seed.getStatus() == SeedStatusEnum.ACTIVE) {
                    status = "参赛中";
                }
                md.append(status).append(" |\n");
            }
            md.append("\n");
        }

        return md.toString();
    }

    private String formatPlayerName(PlayerVO player) {
        if (player == null) {
            return "待定";
        }

        StringBuilder sb = new StringBuilder();
        if (player.getSeed() != null) {
            sb.append("[").append(player.getSeed()).append("] ");
        }

        sb.append(player.getName());

        if (player.getCountry() != null && StringUtils.isNotBlank(player.getCountry().getCode())) {
            sb.append(" (").append(player.getCountry().getCode()).append(")");
        }

        return sb.toString();
    }
}
