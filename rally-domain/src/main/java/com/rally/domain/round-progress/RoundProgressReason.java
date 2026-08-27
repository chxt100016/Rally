package com.rally.domain.tournament.roundprogress;

/** Stable reason attached to an accepted round-progress decision. */
public enum RoundProgressReason {
    QUALIFIER_MATCHES_PENDING,
    MAIN_SLOTS_PENDING,
    ROUND_MATCHES_PENDING,
    ROUND_READY,
    FINAL_REMAINS
}
