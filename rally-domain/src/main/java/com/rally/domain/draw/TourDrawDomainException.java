package com.rally.domain.tour.draw;

/** 签表聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class TourDrawDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TourDrawDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public TourDrawDomainException(
            String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
