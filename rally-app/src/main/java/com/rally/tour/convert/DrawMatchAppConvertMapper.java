package com.rally.tour.convert;

import com.rally.client.tourtv.model.AtpDrawsResponse;
import com.rally.tour.model.Match;
import com.rally.domain.tour.model.MatchStatus;
import com.rally.tour.model.SetScore;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Mapper
public interface DrawMatchAppConvertMapper {

    DrawMatchAppConvertMapper INSTANCE = Mappers.getMapper(DrawMatchAppConvertMapper.class);

    @Mapping(target = "matchId", expression = "java(getMatchId(fixture))")
    @Mapping(target = "matchIndex", expression = "java(extractMatchNumber(getMatchId(fixture)))")
    @Mapping(target = "player1Id", expression = "java(getPlayer1Id(fixture))")
    @Mapping(target = "player2Id", expression = "java(getPlayer2Id(fixture))")
    @Mapping(target = "playerName1", expression = "java(buildFullName(getPlayer1(fixture)))")
    @Mapping(target = "playerName2", expression = "java(buildFullName(getPlayer2(fixture)))")
    @Mapping(target = "winnerId", expression = "java(getWinnerId(fixture))")
    @Mapping(target = "status", expression = "java(getStatus(fixture))")
    @Mapping(target = "startedAt", expression = "java(parseMatchDate(fixture))")
    @Mapping(target = "endedAt", expression = "java(parseMatchDate(fixture))")
    @Mapping(target = "durationMinutes", expression = "java(parseDuration(fixture.getResult()))")
    @Mapping(target = "court", expression = "java(getCourtName(fixture))")
    @Mapping(target = "roundName", ignore = true)
    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "roundNumber", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "sets", expression = "java(parseSetResults(fixture))")
    @Mapping(target = "description", expression = "java(fixture.getMetadata().getDescription())")
    @Mapping(target = "matchDate", expression = "java(parseMatchDateToOnlyDate(fixture))")
    Match toMatch(AtpDrawsResponse.Fixture fixture);

    default String getStatus(AtpDrawsResponse.Fixture fixture) {
        if (fixture == null) return null;
        String resultReason = fixture.getResult().getResultReason();
        if (StringUtils.isBlank(resultReason)) {
            return null;
        }

        return "NOT_FINISHED".equals(resultReason) ? MatchStatus.PENDING.name() : MatchStatus.FINISHED.name();
    }

    default String getMatchId(AtpDrawsResponse.Fixture fixture) {
        if (fixture == null) return null;
        // 优先从 MatchInfo 获取 MatchId
        if (fixture.getMatch() != null && fixture.getMatch().getMatchId() != null) {
            return fixture.getMatch().getMatchId();
        }
        // 从 ResultInfo 获取 MatchCode
        if (fixture.getResult() != null && fixture.getResult().getMatchCode() != null) {
            String matchCode = fixture.getResult().getMatchCode();
            if (!"scheduled".equals(matchCode) && !"live".equals(matchCode) && !"FINISHED".equals(matchCode)) {
                return matchCode;
            }
        }
        // 从 Fixture 的 MatchCode
        if (fixture.getMatchCode() != null) {
            String matchCode = fixture.getMatchCode();
            if (!"scheduled".equals(matchCode) && !"live".equals(matchCode) && !"FINISHED".equals(matchCode)) {
                return matchCode;
            }
        }
        return null;
    }

    default String getPlayer1Id(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getResult() == null) return null;
        if (fixture.getResult().getTeamTop() == null) return null;
        if (fixture.getResult().getTeamTop().getPlayer() == null) return null;
        return fixture.getResult().getTeamTop().getPlayer().getPlayerId();
    }

    default String getPlayer2Id(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getResult() == null) return null;
        if (fixture.getResult().getTeamBottom() == null) return null;
        if (fixture.getResult().getTeamBottom().getPlayer() == null) return null;
        return fixture.getResult().getTeamBottom().getPlayer().getPlayerId();
    }

    default String getWinnerId(AtpDrawsResponse.Fixture fixture) {
        // 从 MatchInfo.WinningPlayerId 获取胜利者球员 ID
        if (fixture.getMatch() != null && fixture.getMatch().getWinningPlayerId() != null) {
            return fixture.getMatch().getWinningPlayerId();
        }
        if (fixture.getResult() != null && "BYE".equals(fixture.getResult().getResultType())) {
            if (fixture.getResult().getTeamTop() != null) {
                return fixture.getResult().getTeamTop().getPlayer().getPlayerId();
            }
            if (fixture.getResult().getTeamBottom() != null) {
                return fixture.getResult().getTeamBottom().getPlayer().getPlayerId();
            }
        }
        return null;
    }

    default AtpDrawsResponse.PlayerInfo getPlayer1(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getResult() == null) return null;
        if (fixture.getResult().getTeamTop() == null) return null;
        return fixture.getResult().getTeamTop().getPlayer();
    }

    default AtpDrawsResponse.PlayerInfo getPlayer2(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getResult() == null) return null;
        if (fixture.getResult().getTeamBottom() == null) return null;
        return fixture.getResult().getTeamBottom().getPlayer();
    }

    default String buildFullName(AtpDrawsResponse.PlayerInfo info) {
        if (info == null) return null;
        StringBuilder sb = new StringBuilder();
        if (info.getFirstName() != null) sb.append(info.getFirstName());
        if (info.getLastName() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(info.getLastName());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    default LocalDateTime parseMatchDate(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getMatch() == null || fixture.getMatch().getMatchDate() == null) {
            return null;
        }
        try {
            return LocalDateTime.parse(fixture.getMatch().getMatchDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            return null;
        }
    }

    default LocalDate parseMatchDateToOnlyDate(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getMatch() == null) {
            return null;
        }
        if (fixture.getMatch().getMatchDate() == null) {
            return null;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(fixture.getMatch().getMatchDate(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return dateTime.toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }

    default Integer parseDuration(AtpDrawsResponse.ResultInfo result) {
        if (result == null || result.getMatchTime() == null) {
            return null;
        }
        // 格式: "01:26:33" -> 分钟数
        try {
            String[] parts = result.getMatchTime().split(":");
            if (parts.length == 3) {
                int hours = Integer.parseInt(parts[0]);
                int minutes = Integer.parseInt(parts[1]);
                return hours * 60 + minutes;
            }
        } catch (Exception e) {
            // 解析失败返回 null
        }
        return null;
    }

    default String getCourtName(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getMatch() == null) return null;
        return fixture.getMatch().getCourtName();
    }

    // 从 matchId 末尾提取数字，如 MS008 → 8
    default Integer extractMatchNumber(String matchId) {
        if (matchId == null || matchId.isEmpty()) return null;
        String digits = matchId.replaceAll("\\D+", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    default List<SetScore> parseSetResults(AtpDrawsResponse.Fixture fixture) {
        if (fixture.getResult() == null || fixture.getResult().getSetResults() == null) {
            return null;
        }
        List<SetScore> sets = new ArrayList<>();
        for (AtpDrawsResponse.SetResult sr : fixture.getResult().getSetResults()) {
            SetScore setScore = SetScore.builder()
                    .setNumber(sr.getSetNumber())
                    .p1Games(sr.getGamesA())
                    .p2Games(sr.getGamesB())
                    .p1Tiebreak(sr.getTiebreakA())
                    .p2Tiebreak(sr.getTiebreakB())
                    .build();
            sets.add(setScore);
        }
        return sets.isEmpty() ? null : sets;
    }
}
