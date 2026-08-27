package com.rally.domain.tournament.match;

/** C1 原子插入比赛根及参与者的结果。 */
public record TournamentMatchInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }

    public static TournamentMatchInsertResult created(long generatedId) {
        return new TournamentMatchInsertResult(Outcome.CREATED, generatedId);
    }

    public static TournamentMatchInsertResult identityConflict() {
        return new TournamentMatchInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
