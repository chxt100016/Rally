package com.rally.tennis.convert;

import com.rally.client.atp.model.AtpAppDrawResponse;
import com.rally.tennis.model.Match;
import com.rally.tennis.model.MatchStatus;
import com.rally.tennis.model.SetScore;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.ArrayList;
import java.util.List;

@Mapper
public interface AtpAppDrawMatchConvertMapper {

    AtpAppDrawMatchConvertMapper INSTANCE = Mappers.getMapper(AtpAppDrawMatchConvertMapper.class);

    @Mapping(target = "matchId", source = "matchId")
    @Mapping(target = "matchIndex", expression = "java(extractMatchNumber(match.getMatchId()))")
    @Mapping(target = "player1Id", source = "playerId")
    @Mapping(target = "player2Id", source = "opponentId")
    @Mapping(target = "playerName1", expression = "java(buildFullName(match.getPlayerFirstName(), match.getPlayerLastName()))")
    @Mapping(target = "playerName2", expression = "java(buildFullName(match.getOpponentFirstName(), match.getOpponentLastName()))")
    @Mapping(target = "winnerId", source = "winningPlayerId")
    @Mapping(target = "status", expression = "java(getStatus(match))")
    @Mapping(target = "sets", expression = "java(parseSets(match))")
    @Mapping(target = "startedAt", ignore = true)
    @Mapping(target = "endedAt", ignore = true)
    @Mapping(target = "matchDate", ignore = true)
    @Mapping(target = "scheduledAt", ignore = true)
    @Mapping(target = "durationMinutes", ignore = true)
    @Mapping(target = "court", ignore = true)
    @Mapping(target = "roundName", ignore = true)
    @Mapping(target = "tournamentId", ignore = true)
    @Mapping(target = "drawId", ignore = true)
    @Mapping(target = "roundNumber", ignore = true)
    @Mapping(target = "scheduledAtText", ignore = true)
    @Mapping(target = "courtSeq", ignore = true)
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "year", ignore = true)
    Match toMatch(AtpAppDrawResponse.Match match);

    default String getStatus(AtpAppDrawResponse.Match match) {
        if (match == null) return null;
        return match.getWinningPlayerId() != null ? MatchStatus.FINISHED.name() : MatchStatus.PENDING.name();
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

    default String buildFullName(String firstName, String lastName) {
        if (firstName == null && lastName == null) return null;
        StringBuilder sb = new StringBuilder();
        if (firstName != null) sb.append(firstName);
        if (lastName != null) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(lastName);
        }
        return sb.toString();
    }

    default List<SetScore> parseSets(AtpAppDrawResponse.Match match) {
        if (match == null) return null;
        List<SetScore> sets = new ArrayList<>();
        addSet(sets, 1, match.getSet1Player(), match.getSet1Opponent(), match.getSet1Tie());
        addSet(sets, 2, match.getSet2Player(), match.getSet2Opponent(), match.getSet2Tie());
        addSet(sets, 3, match.getSet3Player(), match.getSet3Opponent(), match.getSet3Tie());
        addSet(sets, 4, match.getSet4Player(), match.getSet4Opponent(), match.getSet4Tie());
        addSet(sets, 5, match.getSet5Player(), match.getSet5Opponent(), match.getSet5Tie());
        return sets.isEmpty() ? null : sets;
    }

    private void addSet(List<SetScore> sets, int setNumber, Integer p1, Integer p2, Integer tie) {
        if (p1 == null && p2 == null) return;
        sets.add(SetScore.builder()
                .setNumber(setNumber)
                .p1Games(p1)
                .p2Games(p2)
                .p1Tiebreak(tie)
                .p2Tiebreak(null)
                .build());
    }
}
