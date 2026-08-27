package com.rally.domain.tournament.entry;

/** 报名状态。 */
public enum TournamentEntryStatus {
    WAITING,
    FROZEN,
    IN_MATCH,
    PAYING,
    CHAMPION,
    ELIMINATED,
    WITHDRAWN;

    public boolean isTerminal() {
        return this == CHAMPION || this == ELIMINATED || this == WITHDRAWN;
    }
}
