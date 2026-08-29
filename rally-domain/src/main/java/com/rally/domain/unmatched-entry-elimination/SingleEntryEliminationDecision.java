package com.rally.domain.tournament.unmatchedentryelimination;

/** Outcome of deciding whether the single target entry may be eliminated. */
public enum SingleEntryEliminationDecision {
    ELIGIBLE,
    ENTRY_STATUS_OR_ROUND_INVALID,
    IN_ACTIVE_MATCH,
    INPUT_INVALID
}
