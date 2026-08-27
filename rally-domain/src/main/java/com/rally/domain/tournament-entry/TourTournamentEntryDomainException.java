package com.rally.domain.tour.tournamententry;

/** 签表参赛项聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class TourTournamentEntryDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TourTournamentEntryDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
