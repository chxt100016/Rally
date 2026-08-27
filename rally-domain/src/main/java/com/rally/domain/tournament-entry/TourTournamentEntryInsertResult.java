package com.rally.domain.tour.tournamententry;

/** C1 新建结果；复合自然键竞争由适配器显式收敛。 */
public record TourTournamentEntryInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }

    public static TourTournamentEntryInsertResult created(long generatedId) {
        return new TourTournamentEntryInsertResult(Outcome.CREATED, generatedId);
    }

    public static TourTournamentEntryInsertResult identityConflict() {
        return new TourTournamentEntryInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
