package com.rally.domain.tournament.match;

/** 聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class TournamentMatchDomainException extends RuntimeException {

    private final String errorIdentifier;

    public TournamentMatchDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
