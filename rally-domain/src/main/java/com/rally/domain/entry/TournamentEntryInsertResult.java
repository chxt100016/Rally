package com.rally.domain.tournament.entry;

/** C1 插入结果，显式暴露唯一键竞争。 */
public record TournamentEntryInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }
}
