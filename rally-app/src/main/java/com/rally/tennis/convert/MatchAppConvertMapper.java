package com.rally.tennis.convert;

import com.rally.client.tennistv.model.MatchesResponse;
import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.domain.tennis.model.TennisRoundEnum;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Mapper(uses = {PlayerAppConvertMapper.class})
public interface MatchAppConvertMapper {

    MatchAppConvertMapper INSTANCE = Mappers.getMapper(MatchAppConvertMapper.class);

    @Mapping(target = "tournamentId", expression = "java(info.getTournamentId() != null ? String.valueOf(info.getTournamentId()) : null)")
    @Mapping(target = "year", source = "tournamentYear")
    @Mapping(target = "player1Id", expression = "java(info.getPlayerTeam1() != null ? info.getPlayerTeam1().getPlayerId() : null)")
    @Mapping(target = "player2Id", expression = "java(info.getPlayerTeam2() != null ? info.getPlayerTeam2().getPlayerId() : null)")
    @Mapping(target = "playerName1", expression = "java(buildPlayerName(info.getPlayerTeam1()))")
    @Mapping(target = "playerName2", expression = "java(buildPlayerName(info.getPlayerTeam2()))")
    @Mapping(target = "status", expression = "java(com.rally.tennis.model.MatchStatus.toStatus(info.getStatus()))")
    @Mapping(target = "scheduledAt", expression = "java(parseDateTime(info.getMatchDate()))")
    @Mapping(target = "roundName", expression = "java(com.rally.domain.tennis.model.TennisRoundEnum.of(info.getRound().getLongName()))")
    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "roundNumber", ignore = true)
    @Mapping(target = "winnerId", ignore = true)
    @Mapping(target = "court", ignore = true)
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "sets", ignore = true)
    @Mapping(target = "matchDate", expression = "java(parseMatchDate(info.getMatchDate()))")
    Match toMatch(MatchesResponse.MatchInfo info);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "updateTime", ignore = true)
    @Mapping(target = "matchDate", source = "matchDate")
    TennisMatchPO toMatchPO(Match match);

    List<TennisMatchPO> toMatchPOList(List<Match> matches);

    default String buildPlayerName(MatchesResponse.PlayerTeam team) {
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

    default java.time.LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) return null;
        try {
            return java.time.LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            return null;
        }
    }

    default LocalDate parseMatchDate(String matchDateStr) {
        if (matchDateStr == null || matchDateStr.isEmpty()) return null;
        try {
            LocalDateTime dateTime = LocalDateTime.parse(matchDateStr);
            return dateTime.toLocalDate();
        } catch (Exception e) {
            return null;
        }
    }
}
