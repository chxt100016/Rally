package com.rally.domain.tournament.match;

import java.time.LocalDateTime;

/** 与 rally_tournament_match 一行对应的不可变聚合根状态。 */
public record TournamentMatchState(
        Long id,
        String bizId,
        String tournamentId,
        int matchNo,
        TournamentMatchRound round,
        int groupSize,
        String courtBookerId,
        LocalDateTime courtBookerSelectedTime,
        LocalDateTime scheduleSubmittedTime,
        String meetupId,
        Integer winnerEntryNo,
        String submittedBy,
        LocalDateTime submittedTime,
        TournamentMatchRejectPhase rejectPhase,
        String rejectReasonCode,
        String rejectedBy,
        LocalDateTime rejectedTime,
        String lastRebookBy,
        String lastRebookReasonCode,
        LocalDateTime lastRebookTime,
        TournamentMatchStatus status,
        LocalDateTime matchedTime,
        LocalDateTime completedTime,
        int version,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TournamentMatchState withGeneratedId(long generatedId) {
        return copy(generatedId, courtBookerId, courtBookerSelectedTime,
                scheduleSubmittedTime, meetupId, winnerEntryNo, submittedBy,
                submittedTime, rejectPhase, rejectReasonCode, rejectedBy,
                rejectedTime, lastRebookBy, lastRebookReasonCode, lastRebookTime,
                status, completedTime, version);
    }

    TournamentMatchState bookBy(String userId, LocalDateTime selectedTime) {
        return copy(id, userId, selectedTime, scheduleSubmittedTime, meetupId,
                winnerEntryNo, submittedBy, submittedTime, rejectPhase,
                rejectReasonCode, rejectedBy, rejectedTime, lastRebookBy,
                lastRebookReasonCode, lastRebookTime, TournamentMatchStatus.BOOKING,
                completedTime, version + 1);
    }

    TournamentMatchState schedule(String replacementMeetupId, LocalDateTime time) {
        return copy(id, courtBookerId, courtBookerSelectedTime, time,
                replacementMeetupId, winnerEntryNo, submittedBy, submittedTime,
                rejectPhase, rejectReasonCode, rejectedBy, rejectedTime,
                lastRebookBy, lastRebookReasonCode, lastRebookTime,
                TournamentMatchStatus.SCHEDULED, completedTime, version + 1);
    }

    TournamentMatchState withStatus(TournamentMatchStatus replacement) {
        return copy(id, courtBookerId, courtBookerSelectedTime,
                scheduleSubmittedTime, meetupId, winnerEntryNo, submittedBy,
                submittedTime, rejectPhase, rejectReasonCode, rejectedBy,
                rejectedTime, lastRebookBy, lastRebookReasonCode, lastRebookTime,
                replacement, completedTime, version + 1);
    }

    TournamentMatchState rebook(String userId, String reason, LocalDateTime time) {
        return copy(id, courtBookerId, courtBookerSelectedTime,
                scheduleSubmittedTime, meetupId, winnerEntryNo, submittedBy,
                submittedTime, rejectPhase, rejectReasonCode, rejectedBy,
                rejectedTime, userId, reason, time, TournamentMatchStatus.BOOKING,
                completedTime, version + 1);
    }

    TournamentMatchState submitResult(
            int winner,
            String submitter,
            LocalDateTime time) {
        return copy(id, courtBookerId, courtBookerSelectedTime,
                scheduleSubmittedTime, meetupId, winner, submitter, time,
                rejectPhase, rejectReasonCode, rejectedBy, rejectedTime,
                lastRebookBy, lastRebookReasonCode, lastRebookTime,
                TournamentMatchStatus.PENDING_CONFIRM, completedTime, version + 1);
    }

    TournamentMatchState complete(LocalDateTime time) {
        return copy(id, courtBookerId, courtBookerSelectedTime,
                scheduleSubmittedTime, meetupId, winnerEntryNo, submittedBy,
                submittedTime, rejectPhase, rejectReasonCode, rejectedBy,
                rejectedTime, lastRebookBy, lastRebookReasonCode, lastRebookTime,
                TournamentMatchStatus.COMPLETED, time, version + 1);
    }

    TournamentMatchState reject(RejectTournamentMatchCommand command) {
        return copy(id, courtBookerId, courtBookerSelectedTime,
                scheduleSubmittedTime, meetupId, winnerEntryNo, submittedBy,
                submittedTime, command.phase(), command.reasonCode(),
                command.rejectedBy(), command.rejectedTime(), lastRebookBy,
                lastRebookReasonCode, lastRebookTime,
                TournamentMatchStatus.REJECTED, completedTime, version + 1);
    }

    private TournamentMatchState copy(
            Long replacementId,
            String replacementCourtBookerId,
            LocalDateTime replacementCourtBookerSelectedTime,
            LocalDateTime replacementScheduleSubmittedTime,
            String replacementMeetupId,
            Integer replacementWinnerEntryNo,
            String replacementSubmittedBy,
            LocalDateTime replacementSubmittedTime,
            TournamentMatchRejectPhase replacementRejectPhase,
            String replacementRejectReasonCode,
            String replacementRejectedBy,
            LocalDateTime replacementRejectedTime,
            String replacementLastRebookBy,
            String replacementLastRebookReasonCode,
            LocalDateTime replacementLastRebookTime,
            TournamentMatchStatus replacementStatus,
            LocalDateTime replacementCompletedTime,
            int replacementVersion) {
        return new TournamentMatchState(
                replacementId, bizId, tournamentId, matchNo, round, groupSize,
                replacementCourtBookerId, replacementCourtBookerSelectedTime,
                replacementScheduleSubmittedTime, replacementMeetupId,
                replacementWinnerEntryNo, replacementSubmittedBy,
                replacementSubmittedTime, replacementRejectPhase,
                replacementRejectReasonCode, replacementRejectedBy,
                replacementRejectedTime, replacementLastRebookBy,
                replacementLastRebookReasonCode, replacementLastRebookTime,
                replacementStatus, matchedTime, replacementCompletedTime,
                replacementVersion, createTime, updateTime);
    }
}
