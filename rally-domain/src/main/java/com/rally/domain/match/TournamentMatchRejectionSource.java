package com.rally.domain.tournament.match;

/** C8 的终止来源；退赛与自动超时允许不记录人工拒绝审计。 */
public enum TournamentMatchRejectionSource {
    USER,
    TIMEOUT,
    WITHDRAWAL
}
