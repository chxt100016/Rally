package com.rally.domain.tournament.match;

/** 赛事比赛生命周期状态。 */
public enum TournamentMatchStatus {
    MATCHED,
    BOOKING,
    SCHEDULED,
    PENDING_PLAY,
    PENDING_CONFIRM,
    COMPLETED,
    REJECTED;

    public boolean isTerminal() {
        return this == COMPLETED || this == REJECTED;
    }
}
