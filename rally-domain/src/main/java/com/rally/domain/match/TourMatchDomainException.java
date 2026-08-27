package com.rally.domain.tour.match;

/** 巡回赛比赛聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class TourMatchDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TourMatchDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
