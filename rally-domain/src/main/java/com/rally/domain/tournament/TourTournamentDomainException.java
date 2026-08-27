package com.rally.domain.tour.tournament;

/** 职业赛事年度聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class TourTournamentDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TourTournamentDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
