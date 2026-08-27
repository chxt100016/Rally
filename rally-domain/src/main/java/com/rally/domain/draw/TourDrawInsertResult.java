package com.rally.domain.tour.draw;

/** C1 插入结果；自然键竞争由 IDENTITY_CONFLICT 显式返回。 */
public record TourDrawInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }

    public static TourDrawInsertResult created(long generatedId) {
        return new TourDrawInsertResult(Outcome.CREATED, generatedId);
    }

    public static TourDrawInsertResult identityConflict() {
        return new TourDrawInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
