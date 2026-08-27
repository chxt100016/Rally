package com.rally.domain.tour.match;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 与 {@code tour_match} 一行对应的不可变聚合状态。 */
public record TourMatchState(
        Long id,
        String matchId,
        Integer matchIndex,
        long drawId,
        String tournamentId,
        int year,
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
        String setsJson,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TourMatchIdentity identity() {
        return TourMatchIdentity.fromSource(drawId, matchId);
    }

    TourMatchState withGeneratedId(long generatedId) {
        return new TourMatchState(
                generatedId, matchId, matchIndex, drawId, tournamentId, year,
                roundNumber, roundName, player1Id, player2Id, winnerId,
                scheduledAt, scheduledAtText, startedAt, endedAt, court, courtSeq,
                status, durationMinutes, description, matchDate, setsJson,
                createTime, updateTime);
    }
}
