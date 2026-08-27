package com.rally.domain.tour.tournamententry;

import java.time.LocalDateTime;

/** 与 {@code tour_tournament_entry} 一行对应的不可变聚合状态。 */
public record TourTournamentEntryState(
        Long id,
        Long drawId,
        String playerId,
        Short seed,
        String entryType,
        TourTournamentEntryStatus status,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TourTournamentEntryIdentity identity() {
        return TourTournamentEntryIdentity.of(drawId, playerId);
    }

    TourTournamentEntryState withGeneratedId(long generatedId) {
        return new TourTournamentEntryState(
                generatedId,
                drawId,
                playerId,
                seed,
                entryType,
                status,
                createTime,
                updateTime);
    }

    TourTournamentEntryState merge(TourTournamentEntryQualificationPatch patch) {
        return new TourTournamentEntryState(
                id,
                drawId,
                playerId,
                choose(patch.seed(), seed),
                choose(patch.entryType(), entryType),
                status,
                createTime,
                updateTime);
    }

    TourTournamentEntryState withStatus(TourTournamentEntryStatus nextStatus) {
        return new TourTournamentEntryState(
                id,
                drawId,
                playerId,
                seed,
                entryType,
                nextStatus,
                createTime,
                updateTime);
    }

    private static <T> T choose(T patchValue, T existingValue) {
        return patchValue == null ? existingValue : patchValue;
    }
}
