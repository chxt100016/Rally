package com.rally.domain.tour.tournament;

/** C1 插入结果；自然键竞争由 IDENTITY_CONFLICT 显式返回。 */
public record TourTournamentInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }

    public static TourTournamentInsertResult created(long generatedId) {
        return new TourTournamentInsertResult(Outcome.CREATED, generatedId);
    }

    public static TourTournamentInsertResult identityConflict() {
        return new TourTournamentInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
