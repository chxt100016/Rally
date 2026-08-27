package com.rally.domain.tournament.entry;

/** 赛事报名聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class TournamentEntryDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TournamentEntryDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
