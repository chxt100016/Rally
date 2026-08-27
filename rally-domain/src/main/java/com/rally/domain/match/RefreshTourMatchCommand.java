package com.rally.domain.tour.match;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** C1：新增或用任意非空来源字段刷新一场比赛快照。 */
public record RefreshTourMatchCommand(
        Long drawId,
        String matchId,
        String tournamentId,
        Integer year,
        Integer matchIndex,
        Integer roundNumber,
        String roundName,
        String player1Id,
        String player2Id,
        String winnerId,
        LocalDateTime scheduledAt,
        String scheduledAtText,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        String court,
        Integer courtSeq,
        String status,
        Integer durationMinutes,
        String description,
        LocalDate matchDate,
        String setsJson) {
}
