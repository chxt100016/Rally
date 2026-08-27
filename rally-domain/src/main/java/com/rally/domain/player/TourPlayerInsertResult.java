package com.rally.domain.tour.player;

/** C1 新建结果；复合自然键竞争由适配器显式收敛。 */
public record TourPlayerInsertResult(Outcome outcome, Long generatedId) {

    public enum Outcome {
        CREATED,
        IDENTITY_CONFLICT
    }

    public static TourPlayerInsertResult created(long generatedId) {
        return new TourPlayerInsertResult(Outcome.CREATED, generatedId);
    }

    public static TourPlayerInsertResult identityConflict() {
        return new TourPlayerInsertResult(Outcome.IDENTITY_CONFLICT, null);
    }
}
