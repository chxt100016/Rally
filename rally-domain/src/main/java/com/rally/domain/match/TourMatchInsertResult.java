package com.rally.domain.tour.match;

/** C1 新建结果；自然键竞争由适配器显式收敛。 */
public record TourMatchInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }

    public static TourMatchInsertResult created(long generatedId) {
        return new TourMatchInsertResult(Outcome.CREATED, generatedId);
    }

    public static TourMatchInsertResult identityConflict() {
        return new TourMatchInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
