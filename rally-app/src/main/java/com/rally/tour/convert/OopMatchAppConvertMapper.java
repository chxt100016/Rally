package com.rally.tour.convert;

import com.rally.client.tourtv.model.AtpOopResponse;
import com.rally.tour.model.Match;
import org.apache.commons.lang3.StringUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Mapper
public interface OopMatchAppConvertMapper {

    OopMatchAppConvertMapper INSTANCE = Mappers.getMapper(OopMatchAppConvertMapper.class);

    @Mapping(target = "matchId", source = "matchId")
    @Mapping(target = "matchIndex", expression = "java(extractMatchNumber(detail.getMatchId()))")
    @Mapping(target = "tournamentId", expression = "java(detail.getTournamentId() != null ? String.valueOf(detail.getTournamentId()) : null)")
    @Mapping(target = "year", source = "tournamentYear")
    @Mapping(target = "player1Id", expression = "java(getPlayerId(detail, 1))")
    @Mapping(target = "player2Id", expression = "java(getPlayerId(detail, 2))")
    @Mapping(target = "playerName1", expression = "java(buildPlayerName(detail, 1))")
    @Mapping(target = "playerName2", expression = "java(buildPlayerName(detail, 2))")
    @Mapping(target = "status", expression = "java(com.rally.domain.tour.model.MatchStatus.toStatus(detail.getStatus()))")
    @Mapping(target = "winnerId", expression = "java(detail.getWinningPlayerId())")
    @Mapping(target = "scheduledAt", expression = "java(parseScheduledAt(detail.getMatchDate(), detail.getNotBeforeISOTime()))")
    @Mapping(target = "scheduledAtText", expression = "java(parseNotBeforeText(detail))")
    @Mapping(target = "court", source = "courtName")
    @Mapping(target = "courtSeq", source = "courtSeq")
    @Mapping(target = "roundName", expression = "java(resolveRoundName(detail.getRound()))")
    @Mapping(target = "roundNumber", ignore = true)
    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "sets", ignore = true)
    @Mapping(target = "matchDate", expression = "java(parseMatchDate(detail.getMatchDate()))")
    Match toMatch(AtpOopResponse.MatchDetail detail);

    default String getPlayerId(AtpOopResponse.MatchDetail detail, int teamNumber) {
        AtpOopResponse.PlayerTeam legacyTeam = legacyTeam(detail, teamNumber);
        if (legacyTeam != null && StringUtils.isNotBlank(legacyTeam.getPlayerId())) {
            return legacyTeam.getPlayerId();
        }
        AtpOopResponse.TeamPlayer player = firstPlayer(detail, teamNumber);
        return player != null ? player.getPlayerId() : null;
    }

    default String buildPlayerName(AtpOopResponse.MatchDetail detail, int teamNumber) {
        AtpOopResponse.PlayerTeam legacyTeam = legacyTeam(detail, teamNumber);
        if (legacyTeam != null && StringUtils.isNotBlank(legacyTeam.getPlayerId())) {
            return buildPlayerName(legacyTeam);
        }
        AtpOopResponse.TeamPlayer player = firstPlayer(detail, teamNumber);
        if (player == null) return null;
        return joinName(player.getFirstName(), player.getLastName());
    }

    default AtpOopResponse.PlayerTeam legacyTeam(AtpOopResponse.MatchDetail detail, int teamNumber) {
        if (detail == null) return null;
        return teamNumber == 1 ? detail.getPlayerTeam1() : detail.getPlayerTeam2();
    }

    default AtpOopResponse.TeamPlayer firstPlayer(AtpOopResponse.MatchDetail detail, int teamNumber) {
        if (detail == null || detail.getTeamsInMatch() == null) return null;
        return detail.getTeamsInMatch().stream()
                .filter(team -> Integer.valueOf(teamNumber).equals(team.getTeamNumber()))
                .filter(team -> team.getPlayers() != null && !team.getPlayers().isEmpty())
                .flatMap(team -> team.getPlayers().stream()
                        .sorted(java.util.Comparator.comparing(
                                AtpOopResponse.TeamPlayer::getOrderInTeam,
                                java.util.Comparator.nullsLast(Integer::compareTo)))
                        .limit(1))
                .findFirst()
                .orElse(null);
    }

    default String resolveRoundName(AtpOopResponse.RoundInfo round) {
        if (round == null) return null;
        String name = StringUtils.isNotBlank(round.getLongName()) ? round.getLongName() : round.getRoundName();
        return com.rally.domain.tour.model.TourRoundEnum.of(name);
    }

    default String buildPlayerName(AtpOopResponse.PlayerTeam team) {
        if (team == null) return null;
        StringBuilder sb = new StringBuilder();
        if (team.getPlayerFirstNameFull() != null) {
            sb.append(team.getPlayerFirstNameFull());
        } else if (team.getPlayerFirstName() != null) {
            sb.append(team.getPlayerFirstName());
        }
        if (team.getPlayerLastName() != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(team.getPlayerLastName());
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    default String joinName(String firstName, String lastName) {
        if (StringUtils.isBlank(firstName)) return StringUtils.defaultIfBlank(lastName, null);
        if (StringUtils.isBlank(lastName)) return firstName;
        return firstName + " " + lastName;
    }

    @Named("parseNotBeforeText")
    default String parseNotBeforeText(AtpOopResponse.MatchDetail detail) {
        if (Objects.isNull(detail.getNotBeforeText())) {
            return null;
        }
        return com.rally.domain.tour.model.ScheduledAtTextEnum.fromText(detail.getNotBeforeText());
    }

    default java.time.LocalDateTime parseScheduledAt(String matchDate, String notBeforeISOTime) {
        if (StringUtils.isBlank(matchDate) || StringUtils.isBlank(notBeforeISOTime)) return null;
        try {
            // 支持 "2026-05-08" 和 "2026-05-08T00:00:00" 两种格式
            java.time.LocalDate date = matchDate.length() > 10
                    ? java.time.LocalDate.parse(matchDate.substring(0, 10))
                    : java.time.LocalDate.parse(matchDate);
            // notBeforeISOTime 格式: "13:00+0200"，时间部分固定 HH:mm
            java.time.LocalTime time = java.time.LocalTime.parse(
                    notBeforeISOTime.substring(0, 5),
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
            String offsetStr = notBeforeISOTime.substring(5);
            // 支持 "+0000" 格式（转换为 "+00:00"）
            if (offsetStr.length() == 5 && (offsetStr.startsWith("+") || offsetStr.startsWith("-"))) {
                offsetStr = offsetStr.substring(0, 3) + ":" + offsetStr.substring(3);
            }
            java.time.ZoneOffset offset = java.time.ZoneOffset.of(offsetStr);
            return java.time.LocalDateTime.of(date, time)
                    .atOffset(offset)
                    .toZonedDateTime()
                    .withZoneSameInstant(java.time.ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    default LocalDate parseMatchDate(String matchDateStr) {
        if (matchDateStr == null || matchDateStr.isEmpty()) return null;
        try {
            // 支持 "2026-05-08" 和 "2026-05-08T10:12:44" 两种格式
            if (matchDateStr.length() > 10) {
                LocalDateTime dateTime = LocalDateTime.parse(matchDateStr, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                return dateTime.toLocalDate();
            }
            return LocalDate.parse(matchDateStr);
        } catch (Exception e) {
            return null;
        }
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
}
