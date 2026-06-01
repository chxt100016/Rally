package com.rally.tennis.parser;

import com.rally.client.wta.WtaClient;
import com.rally.client.wta.model.WtaScheduleResponse;
import com.rally.domain.tennis.model.ScheduledAtTextEnum;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.Player;
import com.rally.tennis.model.SetScore;
import com.rally.tennis.model.TournamentEntry;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * WTA 赛程解析器，对应 /Scores/Schedule 接口。
 * 按赛事+年份拉取赛程，遍历 ScheduleDays / ScheduleCourts / ScheduleMatches 提取比赛数据。
 * 单打（Partner == null）产生 LS DrawResult，双打（Partner != null）产生 LD DrawResult。
 */
@Slf4j
@Component
public class WtaScheduleMatchParser extends MatchParser<WtaScheduleResponse, WtaScheduleResponse.ScheduleData> {

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId CST_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private WtaClient wtaClient;

    @Override
    protected WtaScheduleResponse request(DrawParams params) {
        return wtaClient.getSchedule(params.getTournamentId(), params.getYear());
    }

    @Override
    protected List<DrawResult<WtaScheduleResponse.ScheduleData>> ls(
            WtaScheduleResponse data, DrawParams params) {
        if (data == null || data.getData() == null
                || CollectionUtils.isEmpty(data.getData().getScheduleDays())) {
            return List.of();
        }

        for (WtaScheduleResponse.ScheduleDay scheduleDay : data.getData().getScheduleDays()) {
            for (WtaScheduleResponse.ScheduleCourt scheduleCourt : scheduleDay.getScheduleCourts()) {
                List<WtaScheduleResponse.ScheduleMatch> filtered = scheduleCourt.getScheduleMatches().stream().filter(item -> item.getMatchId().startsWith("LS")).toList();
                scheduleCourt.setScheduleMatches(filtered);
            }
        }

        return List.of(new DrawResult<>(data.getData(), Discipline.SINGLES, "LS",
                new DrawMeta(null, null), params.getTournamentId(), params.getYear()));
    }



    @Override
    public List<Match> getMatches(DrawResult<WtaScheduleResponse.ScheduleData> draw,
                                  String tournamentId, Long drawId) {
        WtaScheduleResponse.ScheduleData scheduleData = draw.getSlice();
        if (scheduleData == null || CollectionUtils.isEmpty(scheduleData.getScheduleDays())) {
            return List.of();
        }

        boolean isDoubles = draw.getDiscipline() == Discipline.DOUBLES;
        List<Match> matches = new ArrayList<>();

        for (WtaScheduleResponse.ScheduleDay day : scheduleData.getScheduleDays()) {
            LocalDate matchDate = parseDate(day.getMatchDate());
            if (CollectionUtils.isEmpty(day.getScheduleCourts())) continue;

            for (WtaScheduleResponse.ScheduleCourt court : day.getScheduleCourts()) {
                if (CollectionUtils.isEmpty(court.getScheduleMatches())) continue;

                // 同一场地按顺序追踪上一场开始时间，用于 AFTER_PREVIOUS 推算
                LocalDateTime prevScheduledAt = null;

                for (WtaScheduleResponse.ScheduleMatch m : court.getScheduleMatches()) {
                    // 按 Partner 字段区分单打/双打，与当前 draw 类型对应
                    boolean matchIsDoubles = m.getPartner() != null;
                    if (matchIsDoubles != isDoubles) continue;

                    Match match = new Match();
                    match.setMatchId(m.getMatchId());
                    match.setTournamentId(tournamentId);
                    match.setYear(draw.getYear());
                    match.setDrawId(drawId);
                    match.setMatchDate(matchDate);
                    match.setCourt(court.getCourtName());

                    if (m.getRound() != null) {
                        match.setRoundName(m.getRound().getShortName());
                        match.setRoundNumber(parseRoundId(m.getRound().getId()));
                    }

                    match.setStatus(MatchStatus.toStatus(m.getMatchState()));
                    String scheduledAtText = ScheduledAtTextEnum.fromText(m.getNotBeforeText());
                    match.setScheduledAtText(scheduledAtText);

                    // AFTER_PREVIOUS：用上一场时间 +1h10m 推算；否则解析原始 UTC 时间
                    LocalDateTime scheduledAt;
                    if (ScheduledAtTextEnum.AFTER_PREVIOUS.name().equals(scheduledAtText) && prevScheduledAt != null) {
                        scheduledAt = prevScheduledAt.plusMinutes(70);
                    } else {
                        scheduledAt = parseUtcDateTime(m.getMatchTimeUtcIsoDateTime());
                    }
                    match.setScheduledAt(scheduledAt);
                    if (scheduledAt != null && "F".equals(m.getMatchState())) {
                        match.setEndedAt(scheduledAt);
                    }
                    prevScheduledAt = scheduledAt;

                    // 球员
                    if (m.getPlayer() != null) {
                        match.setPlayer1Id(m.getPlayer().getPlayerId());
                        match.setPlayerName1(m.getPlayer().getPlayerFirstName()
                                + " " + m.getPlayer().getPlayerLastName());
                    }
                    if (m.getOpponent() != null) {
                        match.setPlayer2Id(m.getOpponent().getPlayerId());
                        match.setPlayerName2(m.getOpponent().getPlayerFirstName()
                                + " " + m.getOpponent().getPlayerLastName());
                    }

                    // 获胜者
                    match.setWinnerId(StringUtils.isBlank(m.getWinner()) ? null : m.getWinningPlayerId());

                    // 比分
                    match.setSets(parseSets(m.getMatchScores()));

                    matches.add(match);
                }
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<WtaScheduleResponse.ScheduleData> draw) {
        WtaScheduleResponse.ScheduleData scheduleData = draw.getSlice();
        if (scheduleData == null || CollectionUtils.isEmpty(scheduleData.getScheduleDays())) {
            return List.of();
        }

        List<Player> players = new ArrayList<>();
        for (WtaScheduleResponse.ScheduleDay day : scheduleData.getScheduleDays()) {
            if (CollectionUtils.isEmpty(day.getScheduleCourts())) continue;
            for (WtaScheduleResponse.ScheduleCourt court : day.getScheduleCourts()) {
                if (CollectionUtils.isEmpty(court.getScheduleMatches())) continue;
                for (WtaScheduleResponse.ScheduleMatch m : court.getScheduleMatches()) {
                    addPlayer(players, m.getPlayer());
                    addPlayer(players, m.getOpponent());
                    addPlayer(players, m.getPartner());
                    addPlayer(players, m.getOpponentPartner());
                }
            }
        }
        return players;
    }

    @Override
    public List<TournamentEntry> getEntries(DrawResult<WtaScheduleResponse.ScheduleData> draw,
                                            Long drawId) {
        WtaScheduleResponse.ScheduleData scheduleData = draw.getSlice();
        if (scheduleData == null || CollectionUtils.isEmpty(scheduleData.getScheduleDays())) {
            return List.of();
        }

        List<TournamentEntry> entries = new ArrayList<>();
        for (WtaScheduleResponse.ScheduleDay day : scheduleData.getScheduleDays()) {
            if (CollectionUtils.isEmpty(day.getScheduleCourts())) continue;
            for (WtaScheduleResponse.ScheduleCourt court : day.getScheduleCourts()) {
                if (CollectionUtils.isEmpty(court.getScheduleMatches())) continue;
                for (WtaScheduleResponse.ScheduleMatch m : court.getScheduleMatches()) {
                    addEntry(entries, drawId, m.getPlayer(), m.getSeedTeam1(), m.getEntryTypeTeam1());
                    addEntry(entries, drawId, m.getOpponent(), m.getSeedTeam2(), m.getEntryTypeTeam2());
                }
            }
        }
        return entries;
    }

    @Override
    public CollectType collectType() {
        return CollectType.WTA_SCHEDULE;
    }

    // --- 私有工具方法 ---


    private LocalDateTime parseUtcDateTime(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            // 原始字符串为 UTC 时间，转换为中国时区（UTC+8）存储
            return LocalDateTime.parse(raw, UTC_FORMATTER)
                    .atZone(UTC_ZONE)
                    .withZoneSameInstant(CST_ZONE)
                    .toLocalDateTime();
        } catch (DateTimeParseException e) {
            log.debug("无法解析 MatchTimeUtcIsoDateTime: {}", raw);
            return null;
        }
    }

    private LocalDate parseDate(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
            // MatchDate 格式 "2026-05-21T00:00:00"
            return LocalDateTime.parse(raw).toLocalDate();
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException ex) {
                log.debug("无法解析 MatchDate: {}", raw);
                return null;
            }
        }
    }

    private Integer parseRoundId(String roundId) {
        if (roundId == null || roundId.isEmpty()) return null;
        try {
            return Integer.parseInt(roundId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<SetScore> parseSets(WtaScheduleResponse.MatchScores scores) {
        if (scores == null) return null;
        String[][] setData = {
            {scores.getScoreSet1A(), scores.getScoreSet1B(), scores.getScoreTBSet1()},
            {scores.getScoreSet2A(), scores.getScoreSet2B(), scores.getScoreTBSet2()},
            {scores.getScoreSet3A(), scores.getScoreSet3B(), scores.getScoreTBSet3()},
            {scores.getScoreSet4A(), scores.getScoreSet4B(), scores.getScoreTBSet4()},
            {scores.getScoreSet5A(), scores.getScoreSet5B(), scores.getScoreTBSet5()},
        };
        List<SetScore> sets = new ArrayList<>();
        for (int i = 0; i < setData.length; i++) {
            String a = setData[i][0], b = setData[i][1], tb = setData[i][2];
            if (a == null || a.isEmpty()) break;
            SetScore s = new SetScore();
            s.setSetNumber(i + 1);
            try { s.setP1Games(Integer.parseInt(a)); } catch (NumberFormatException ignored) {}
            try { s.setP2Games(Integer.parseInt(b)); } catch (NumberFormatException ignored) {}
            if (tb != null && !tb.isEmpty()) {
                try { s.setP1Tiebreak(Integer.parseInt(tb)); } catch (NumberFormatException ignored) {}
            }
            sets.add(s);
        }
        return sets.isEmpty() ? null : sets;
    }

    private void addPlayer(List<Player> players, WtaScheduleResponse.PlayerInfo info) {
        if (info == null || info.getPlayerId() == null || info.getPlayerId().isEmpty()) return;
        Player player = new Player();
        player.setPlayerId(info.getPlayerId());
        player.setFirstName(info.getPlayerFirstName());
        player.setLastName(info.getPlayerLastName());
        player.setNationality(info.getCountry());
        players.add(player);
    }

    private void addEntry(List<TournamentEntry> entries, Long drawId,
                          WtaScheduleResponse.PlayerInfo info,
                          String seedStr, String entryType) {
        if (info == null || info.getPlayerId() == null || info.getPlayerId().isEmpty()) return;
        TournamentEntry entry = new TournamentEntry();
        entry.setPlayerId(info.getPlayerId());
        entry.setDrawId(drawId);
        if (seedStr != null && !seedStr.isEmpty()) {
            try { entry.setSeed(Short.parseShort(seedStr)); } catch (NumberFormatException ignored) {}
        }
        if (entryType != null && !entryType.isEmpty()) {
            entry.setEntryType(entryType);
        }
        entries.add(entry);
    }
}
