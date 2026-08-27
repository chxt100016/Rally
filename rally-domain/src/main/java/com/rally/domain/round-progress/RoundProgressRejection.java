package com.rally.domain.tournament.roundprogress;

/** Explicit rejection reasons for snapshots that cannot produce a progress decision. */
public enum RoundProgressRejection {
    TOURNAMENT_NOT_FOUND,
    TOTAL_SLOTS_UNSUPPORTED
}
