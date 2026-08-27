package com.rally.domain.tournament.entry;

import java.time.LocalDateTime;
import java.util.List;

/** 与 {@code rally_tournament_entry} 一行对应的不可变聚合状态。 */
public record TournamentEntryState(
        Long id,
        String bizId,
        String tournamentId,
        String userId,
        String partnerId,
        int entryNo,
        List<String> preferredDistricts,
        TournamentEntryCourtAbility courtAbility,
        List<String> availableTimes,
        TournamentEntryStage stage,
        TournamentEntryStatus status,
        TournamentEntryRound currentRound,
        int qualifierRejectCount,
        int mainDrawRejectCount,
        LocalDateTime qualifiedTime,
        LocalDateTime paidTime,
        LocalDateTime lastVisitTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TournamentEntryIdentity identity() {
        return new TournamentEntryIdentity(
                bizId, tournamentId, userId, entryNo);
    }

    TournamentEntryPreferences preferences() {
        return new TournamentEntryPreferences(
                preferredDistricts, courtAbility, availableTimes);
    }

    TournamentEntryState withGeneratedId(long generatedId) {
        return copy(generatedId, preferredDistricts, courtAbility, availableTimes,
                stage, status, currentRound, qualifierRejectCount, mainDrawRejectCount,
                qualifiedTime, paidTime, lastVisitTime);
    }

    TournamentEntryState withPreferences(TournamentEntryPreferences preferences) {
        return copy(id, preferences.preferredDistricts(), preferences.courtAbility(),
                preferences.availableTimes(), stage, status, currentRound,
                qualifierRejectCount, mainDrawRejectCount, qualifiedTime, paidTime,
                lastVisitTime);
    }

    TournamentEntryState withPartnerId(String replacementPartnerId) {
        return new TournamentEntryState(
                id, bizId, tournamentId, userId, replacementPartnerId, entryNo,
                preferredDistricts, courtAbility, availableTimes, stage, status,
                currentRound, qualifierRejectCount, mainDrawRejectCount,
                qualifiedTime, paidTime, lastVisitTime, createTime, updateTime);
    }

    TournamentEntryState withProgress(
            TournamentEntryStage replacementStage,
            TournamentEntryStatus replacementStatus,
            TournamentEntryRound replacementRound,
            int replacementQualifierRejectCount,
            int replacementMainDrawRejectCount,
            LocalDateTime replacementPaidTime) {
        return copy(id, preferredDistricts, courtAbility, availableTimes,
                replacementStage, replacementStatus, replacementRound,
                replacementQualifierRejectCount, replacementMainDrawRejectCount,
                qualifiedTime, replacementPaidTime, lastVisitTime);
    }

    TournamentEntryState withLastVisitTime(LocalDateTime replacement) {
        return copy(id, preferredDistricts, courtAbility, availableTimes,
                stage, status, currentRound, qualifierRejectCount, mainDrawRejectCount,
                qualifiedTime, paidTime, replacement);
    }

    private TournamentEntryState copy(
            Long replacementId,
            List<String> replacementDistricts,
            TournamentEntryCourtAbility replacementAbility,
            List<String> replacementTimes,
            TournamentEntryStage replacementStage,
            TournamentEntryStatus replacementStatus,
            TournamentEntryRound replacementRound,
            int replacementQualifierRejectCount,
            int replacementMainDrawRejectCount,
            LocalDateTime replacementQualifiedTime,
            LocalDateTime replacementPaidTime,
            LocalDateTime replacementLastVisitTime) {
        return new TournamentEntryState(
                replacementId, bizId, tournamentId, userId, partnerId, entryNo,
                replacementDistricts, replacementAbility, replacementTimes,
                replacementStage, replacementStatus, replacementRound,
                replacementQualifierRejectCount, replacementMainDrawRejectCount,
                replacementQualifiedTime, replacementPaidTime, replacementLastVisitTime,
                createTime, updateTime);
    }
}
