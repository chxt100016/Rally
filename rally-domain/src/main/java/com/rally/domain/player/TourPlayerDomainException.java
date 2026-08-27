package com.rally.domain.tour.player;

/** 职业球员聚合拒绝 C1 命令时携带稳定错误标识的领域异常。 */
public final class TourPlayerDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TourPlayerDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
