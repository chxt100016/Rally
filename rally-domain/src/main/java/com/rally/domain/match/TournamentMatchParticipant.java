package com.rally.domain.tournament.match;

import java.time.LocalDateTime;

/** 与 rally_tournament_match_participant 一行对应的不可变实体状态。 */
public record TournamentMatchParticipant(
        Long id,
        String bizId,
        String matchId,
        String tournamentId,
        String userId,
        int entryNo,
        TournamentMatchConfirmStatus confirmStatus,
        LocalDateTime confirmTime,
        TournamentMatchConfirmStatus resultConfirmStatus,
        LocalDateTime resultConfirmTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TournamentMatchParticipant confirmSchedule(LocalDateTime time) {
        return withConfirm(TournamentMatchConfirmStatus.CONFIRMED, time);
    }

    TournamentMatchParticipant rejectSchedule(LocalDateTime time) {
        return withConfirm(TournamentMatchConfirmStatus.REJECTED, time);
    }

    TournamentMatchParticipant resetScheduleConfirmation() {
        return withConfirm(TournamentMatchConfirmStatus.PENDING, null);
    }

    TournamentMatchParticipant confirmResult(LocalDateTime time) {
        return withResultConfirm(TournamentMatchConfirmStatus.CONFIRMED, time);
    }

    TournamentMatchParticipant rejectResult(LocalDateTime time) {
        return withResultConfirm(TournamentMatchConfirmStatus.REJECTED, time);
    }

    TournamentMatchParticipant resetResultConfirmation() {
        return withResultConfirm(TournamentMatchConfirmStatus.PENDING, null);
    }

    private TournamentMatchParticipant withConfirm(
            TournamentMatchConfirmStatus replacement,
            LocalDateTime replacementTime) {
        return new TournamentMatchParticipant(
                id, bizId, matchId, tournamentId, userId, entryNo,
                replacement, replacementTime, resultConfirmStatus, resultConfirmTime,
                createTime, updateTime);
    }

    private TournamentMatchParticipant withResultConfirm(
            TournamentMatchConfirmStatus replacement,
            LocalDateTime replacementTime) {
        return new TournamentMatchParticipant(
                id, bizId, matchId, tournamentId, userId, entryNo,
                confirmStatus, confirmTime, replacement, replacementTime,
                createTime, updateTime);
    }
}
