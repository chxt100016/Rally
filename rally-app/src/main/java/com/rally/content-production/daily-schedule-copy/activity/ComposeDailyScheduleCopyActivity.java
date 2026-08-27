package com.rally.contentproduction.dailyschedulecopy.activity;

import com.rally.domain.tour.TourMatchQueryDomainService;
import com.rally.domain.tour.TourTournamentQueryDomainService;
import com.rally.domain.tour.model.MatchGroupDTO;
import com.rally.domain.tour.model.MatchQueryVO;
import com.rally.domain.tour.model.PlayerVO;
import com.rally.domain.tour.model.SetScoreVO;
import com.rally.domain.tour.model.TournamentData;
import com.rally.domain.tour.model.TournamentGroupData;
import com.rally.domain.translation.gateway.TranslationRepository;
import com.rally.domain.translation.model.TranslationData;
import com.rally.domain.translation.model.TranslationEntityTypeEnum;
import com.rally.domain.translation.model.TranslationKey;
import com.rally.domain.translation.model.TranslationLanguageEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 业务活动 compose-daily-schedule-copy：只读生成当天职业赛事赛程文案及缺译键。
 */
@Component
@RequiredArgsConstructor
public class ComposeDailyScheduleCopyActivity {

    private static final String NO_MATCHES = "暂无比赛";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final TranslationLanguageEnum TARGET_LANGUAGE = TranslationLanguageEnum.ZH_CN;

    private final TourTournamentQueryDomainService tournamentQueryService;
    private final TourMatchQueryDomainService matchQueryService;
    private final TranslationRepository translationRepository;

    public ComposeDailyScheduleCopyResult execute() {
        LocalDate runDate = LocalDate.now();

        // A1 运行日前后一天内的合格赛事由既有领域查询完成类别过滤、传递合组与稳定排序。
        List<TournamentGroupData> tournamentGroups = tournamentQueryService.findValidCurrentTournamentGroups(runDate);
        if (CollectionUtils.isEmpty(tournamentGroups)) {
            return noMatches();
        }

        // A2/A3 按赛事组批量取得比赛、签表报名与球员资料，过滤空签后形成日期和球场结构。
        List<DailyGroup> dailyGroups = tournamentGroups.stream()
                .map(group -> new DailyGroup(group, matchQueryService.upcomingDateGroups(group.getTournamentIds())))
                .filter(group -> CollectionUtils.isNotEmpty(group.dateGroups()))
                .toList();
        if (dailyGroups.isEmpty()) {
            return noMatches();
        }

        List<TournamentData> displayedTournaments = findDisplayedTournaments(dailyGroups);
        if (displayedTournaments.isEmpty()) {
            return noMatches();
        }

        // A4 一次读取全部已有译文；空译文与未命中项保留原文并进入缺译集合，本活动不登记数据。
        Set<TranslationKey> requestedKeys = collectTranslationKeys(dailyGroups, displayedTournaments);
        Map<TranslationKey, String> translations = findTranslations(requestedKeys);
        Set<TranslationKey> missingTranslationKeys = requestedKeys.stream()
                .filter(key -> StringUtils.isBlank(translations.get(key)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        applyMatchTranslations(dailyGroups, translations);

        // A5 沿用既有纯文本格式生成标题、摘要、分日球场段与比赛行。
        String copy = buildCopy(runDate, dailyGroups, displayedTournaments, translations);
        return new ComposeDailyScheduleCopyResult(copy, missingTranslationKeys);
    }

    private ComposeDailyScheduleCopyResult noMatches() {
        return new ComposeDailyScheduleCopyResult(NO_MATCHES, Set.of());
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

    private Set<TranslationKey> collectTranslationKeys(List<DailyGroup> dailyGroups,
                                                       List<TournamentData> tournaments) {
        Set<TranslationKey> keys = new LinkedHashSet<>();
        for (TournamentData tournament : tournaments) {
            addTranslationKey(keys, TranslationEntityTypeEnum.TOURNAMENT, tournament.getName());
        }
        for (DailyGroup dailyGroup : dailyGroups) {
            for (MatchGroupDTO dateGroup : dailyGroup.dateGroups()) {
                if (CollectionUtils.isEmpty(dateGroup.getChildren())) {
                    continue;
                }
                for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
                    addTranslationKey(keys, TranslationEntityTypeEnum.COURT, courtGroup.getName());
                    if (CollectionUtils.isEmpty(courtGroup.getData())) {
                        continue;
                    }
                    for (MatchQueryVO match : courtGroup.getData()) {
                        addPlayerTranslationKey(keys, match.getPlayer1());
                        addPlayerTranslationKey(keys, match.getPlayer2());
                    }
                }
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
        Map<TranslationKey, String> result = new LinkedHashMap<>();
        for (TranslationData translation : translationRepository.findBatch(queries)) {
            if (translation == null || StringUtils.isBlank(translation.getOriginalText())
                    || StringUtils.isBlank(translation.getTranslatedText())) {
                continue;
            }
            TranslationKey key = new TranslationKey(
                    translation.getEntityType(), translation.getOriginalText(), translation.getLanguage());
            result.put(key, translation.getTranslatedText());
        }
        return result;
    }

    private void applyMatchTranslations(List<DailyGroup> dailyGroups, Map<TranslationKey, String> translations) {
        for (DailyGroup dailyGroup : dailyGroups) {
            for (MatchGroupDTO dateGroup : dailyGroup.dateGroups()) {
                if (CollectionUtils.isEmpty(dateGroup.getChildren())) {
                    continue;
                }
                for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
                    courtGroup.setName(translate(TranslationEntityTypeEnum.COURT, courtGroup.getName(), translations));
                    if (CollectionUtils.isEmpty(courtGroup.getData())) {
                        continue;
                    }
                    for (MatchQueryVO match : courtGroup.getData()) {
                        translatePlayer(match.getPlayer1(), translations);
                        translatePlayer(match.getPlayer2(), translations);
                    }
                }
            }
        }
    }

    private void translatePlayer(PlayerVO player, Map<TranslationKey, String> translations) {
        if (player != null) {
            player.setName(translate(TranslationEntityTypeEnum.PLAYER, player.getName(), translations));
        }
    }

    private String buildCopy(LocalDate runDate,
                             List<DailyGroup> dailyGroups,
                             List<TournamentData> displayedTournaments,
                             Map<TranslationKey, String> translations) {
        StringBuilder copy = new StringBuilder();
        copy.append(joinTournamentNames(displayedTournaments, translations)).append("\n");
        copy.append(buildScheduleSummary(displayedTournaments, runDate)).append("\n\n");

        boolean hasScheduleSection = false;
        for (DailyGroup dailyGroup : dailyGroups) {
            for (MatchGroupDTO dateGroup : dailyGroup.dateGroups()) {
                if (CollectionUtils.isEmpty(dateGroup.getChildren())) {
                    continue;
                }
                List<TournamentData> dateTournaments = tournamentsForDate(dailyGroup.group(), dateGroup);
                if (dateTournaments.isEmpty()) {
                    continue;
                }

                if (hasScheduleSection) {
                    copy.append("\n");
                }
                hasScheduleSection = true;
                copy.append(joinTournamentNames(dateTournaments, translations)).append("赛程");
                String dateText = formatDateText(dateGroup.getKey());
                if (StringUtils.isNotBlank(dateText)) {
                    copy.append(" | ").append(dateText);
                    String roundText = extractPrimaryRound(dateGroup.getChildren());
                    if (StringUtils.isNotBlank(roundText)) {
                        copy.append(" ").append(roundText);
                    }
                }
                copy.append("\n");

                for (MatchGroupDTO courtGroup : dateGroup.getChildren()) {
                    if (CollectionUtils.isEmpty(courtGroup.getData())) {
                        continue;
                    }
                    copy.append(StringUtils.defaultString(courtGroup.getName())).append("\n");
                    for (MatchQueryVO match : courtGroup.getData()) {
                        appendMatch(copy, match);
                    }
                }
            }
        }
        return hasScheduleSection ? copy.toString() : NO_MATCHES;
    }

    private String joinTournamentNames(List<TournamentData> tournaments,
                                       Map<TranslationKey, String> translations) {
        return tournaments.stream()
                .map(TournamentData::getName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .map(name -> translate(TranslationEntityTypeEnum.TOURNAMENT, name, translations))
                .collect(Collectors.joining("｜"));
    }

    private String translate(TranslationEntityTypeEnum type,
                             String originalText,
                             Map<TranslationKey, String> translations) {
        if (StringUtils.isBlank(originalText)) {
            return originalText;
        }
        return StringUtils.defaultIfBlank(
                translations.get(new TranslationKey(type, originalText, TARGET_LANGUAGE)), originalText);
    }

    private String buildScheduleSummary(List<TournamentData> tournaments, LocalDate date) {
        Set<String> tours = tournaments.stream()
                .map(TournamentData::getTour)
                .filter(StringUtils::isNotBlank)
                .map(tour -> tour.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        List<String> summary = new ArrayList<>();
        if (tours.contains("ATP")) {
            summary.add("ATP赛程");
        }
        if (tours.contains("WTA")) {
            summary.add("WTA赛程");
        }
        summary.add(formatDateText(date.format(DATE_FORMATTER)));
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

    private void appendMatch(StringBuilder copy, MatchQueryVO match) {
        copy.append(formatPlayerName(match.getPlayer1()))
                .append(" vs ")
                .append(formatPlayerName(match.getPlayer2()));
        if (StringUtils.isNotBlank(match.getScheduledShow())) {
            copy.append(" | ").append(match.getScheduledShow());
        }
        if (CollectionUtils.isNotEmpty(match.getSets())) {
            copy.append(" | ");
            for (SetScoreVO set : match.getSets()) {
                copy.append(set.getPlayer1()).append("-").append(set.getPlayer2()).append(" ");
            }
        }
        if (StringUtils.isNotBlank(match.getStatusLabel())) {
            copy.append(" | ").append(match.getStatusLabel());
        }
        copy.append("\n");
    }

    private String formatDateText(String dateKey) {
        if (StringUtils.isBlank(dateKey)) {
            return "";
        }
        try {
            LocalDate date = LocalDate.parse(dateKey, DATE_FORMATTER);
            return date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
        } catch (RuntimeException ignored) {
            return dateKey;
        }
    }

    private String extractPrimaryRound(List<MatchGroupDTO> courtGroups) {
        if (courtGroups == null) {
            return "";
        }
        for (MatchGroupDTO courtGroup : courtGroups) {
            if (CollectionUtils.isNotEmpty(courtGroup.getData())) {
                String round = courtGroup.getData().get(0).getRoundShow();
                if (StringUtils.isNotBlank(round)) {
                    return round;
                }
            }
        }
        return "";
    }

    private String formatPlayerName(PlayerVO player) {
        if (player == null) {
            return "待定";
        }
        StringBuilder name = new StringBuilder();
        if (player.getCountry() != null && StringUtils.isNotBlank(player.getCountry().getFlagCode())) {
            String flag = countryFlag(player.getCountry().getFlagCode());
            if (StringUtils.isNotBlank(flag)) {
                name.append(flag).append(" ");
            }
        }
        name.append(player.getName());
        if (player.getSeed() != null) {
            name.append("[").append(player.getSeed()).append("]");
        }
        return name.toString();
    }

    private String countryFlag(String iso2Code) {
        if (StringUtils.isBlank(iso2Code) || iso2Code.length() != 2) {
            return "";
        }
        String code = iso2Code.toUpperCase(Locale.ROOT);
        if (!Character.isLetter(code.charAt(0)) || !Character.isLetter(code.charAt(1))) {
            return "";
        }
        int first = Character.codePointAt(code, 0) - 'A' + 0x1F1E6;
        int second = Character.codePointAt(code, 1) - 'A' + 0x1F1E6;
        return new String(Character.toChars(first)) + new String(Character.toChars(second));
    }

    private record DailyGroup(TournamentGroupData group, List<MatchGroupDTO> dateGroups) {
    }
}
