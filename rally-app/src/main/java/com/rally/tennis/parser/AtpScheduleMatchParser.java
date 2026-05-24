package com.rally.tennis.parser;

import com.rally.client.atp.AtpClient;
import com.rally.client.wta.model.WtaScheduleResponse;
import com.rally.domain.tennis.model.ScheduledAtTextEnum;
import com.rally.tennis.model.Discipline;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.Player;
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
 * ATP 赛程备用解析器，对应 app.atptour.com /scores/schedule 接口。
 * 响应结构与 WtaScheduleResponse 相同，按赛事+年份拉取，
 * 过滤 Tour=ATP 且 MatchId 以 "MS" 开头的单打比赛。
 */
@Slf4j
@Component
public class AtpScheduleMatchParser extends MatchParser<WtaScheduleResponse, WtaScheduleResponse.ScheduleData> {

    private static final DateTimeFormatter UTC_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final ZoneId UTC_ZONE = ZoneId.of("UTC");
    private static final ZoneId CST_ZONE = ZoneId.of("Asia/Shanghai");

    @Resource
    private AtpClient atpClient;

    @Override
    protected WtaScheduleResponse fetchData(DrawParams params) {
        return atpClient.getSchedule(params.getTournamentId(), params.getYear());
    }

    /** 过滤出 Tour=ATP 且 MatchId 以 "MS" 开头的单打比赛，产生一个 MS DrawResult */
    @Override
    protected List<DrawResult<WtaScheduleResponse.ScheduleData>> fetchMs(
            WtaScheduleResponse data, DrawParams params) {
        if (data == null || data.getData() == null
                || CollectionUtils.isEmpty(data.getData().getScheduleDays())) {
            return List.of();
        }

        for (WtaScheduleResponse.ScheduleDay day : data.getData().getScheduleDays()) {
            if (CollectionUtils.isEmpty(day.getScheduleCourts())) continue;
            for (WtaScheduleResponse.ScheduleCourt court : day.getScheduleCourts()) {
                if (CollectionUtils.isEmpty(court.getScheduleMatches())) continue;
                // 只保留 ATP 单打比赛
                court.setScheduleMatches(court.getScheduleMatches().stream()
                        .filter(m -> "ATP".equals(m.getTour())
                                && m.getMatchId() != null
                                && m.getMatchId().startsWith("MS"))
                        .toList());
            }
        }

        return List.of(new DrawResult<>(data.getData(), Discipline.SINGLES, "MS",
                new DrawMeta(null, null), params.getTournamentId(), params.getYear()));
    }

    @Override
    public List<Match> getMatches(DrawResult<WtaScheduleResponse.ScheduleData> draw,
                                  String tournamentId, Long drawId) {
        WtaScheduleResponse.ScheduleData scheduleData = draw.getSlice();
        if (scheduleData == null || CollectionUtils.isEmpty(scheduleData.getScheduleDays())) {
            return List.of();
        }

        List<Match> matches = new ArrayList<>();
        for (WtaScheduleResponse.ScheduleDay day : scheduleData.getScheduleDays()) {
            LocalDate matchDate = parseDate(day.getMatchDate());
            if (CollectionUtils.isEmpty(day.getScheduleCourts())) continue;

            for (WtaScheduleResponse.ScheduleCourt court : day.getScheduleCourts()) {
                if (CollectionUtils.isEmpty(court.getScheduleMatches())) continue;

                // 同一场地按顺序追踪上一场开始时间，用于 AFTER_PREVIOUS 推算
                LocalDateTime prevScheduledAt = null;

                for (WtaScheduleResponse.ScheduleMatch m : court.getScheduleMatches()) {
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
                    if (ScheduledAtTextEnum.AFTER_PREVIOUS.name().equals(scheduledAtText)
                            && prevScheduledAt != null) {
                        scheduledAt = prevScheduledAt.plusMinutes(70);
                    } else {
                        scheduledAt = parseUtcDateTime(m.getMatchTimeUtcIsoDateTime());
                    }
                    match.setScheduledAt(scheduledAt);
                    prevScheduledAt = scheduledAt;

                    if (m.getPlayer() != null) {
                        match.setPlayer1Id(m.getPlayer().getPlayerId());
                        match.setPlayerName1(buildName(m.getPlayer()));
                    }
                    if (m.getOpponent() != null) {
                        match.setPlayer2Id(m.getOpponent().getPlayerId());
                        match.setPlayerName2(buildName(m.getOpponent()));
                    }

                    match.setWinnerId(StringUtils.isBlank(m.getWinner()) ? null : m.getWinningPlayerId());

                    matches.add(match);
                }
            }
        }
        return matches;
    }

    @Override
    public List<Player> getPlayers(DrawResult<WtaScheduleResponse.ScheduleData> draw) {
       return List.of();
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
        return CollectType.ATP_SCHEDULE;
    }

    // --- 私有工具方法 ---

    private LocalDateTime parseUtcDateTime(String raw) {
        if (raw == null || raw.isEmpty()) return null;
        try {
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
            // MatchDate 格式 "2026-05-24T00:00:00"
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

    private String buildName(WtaScheduleResponse.PlayerInfo info) {
        if (info == null) return null;
        String first = info.getPlayerFirstName();
        String last = info.getPlayerLastName();
        if (first == null && last == null) return null;
        if (first == null) return last;
        if (last == null) return first;
        return first + " " + last;
    }

    private void addPlayer(List<Player> players, WtaScheduleResponse.PlayerInfo info) {
        if (info == null || StringUtils.isBlank(info.getPlayerId())) return;
        Player player = new Player();
        player.setPlayerId(info.getPlayerId());
        player.setFirstName(info.getPlayerFirstName());
        player.setLastName(info.getPlayerLastName());
        player.setNationality(info.getCountry());
        player.setTour("ATP");
        players.add(player);
    }

    private void addEntry(List<TournamentEntry> entries, Long drawId,
                          WtaScheduleResponse.PlayerInfo info,
                          String seedStr, String entryType) {
        if (info == null || StringUtils.isBlank(info.getPlayerId())) return;
        TournamentEntry entry = new TournamentEntry();
        entry.setPlayerId(info.getPlayerId());
        entry.setDrawId(drawId);
        if (StringUtils.isNotBlank(seedStr)) {
            try { entry.setSeed(Short.parseShort(seedStr)); } catch (NumberFormatException ignored) {}
        }
        if (StringUtils.isNotBlank(entryType)) {
            entry.setEntryType(entryType);
        }
        entries.add(entry);
    }
}
