package com.rally.domain.tournament.unmatchedentryelimination;

/** Read-only identity of a participant in an in-progress tournament match. */
public record ActiveParticipantSnapshot(
        Integer entryNo,
        String userId) {
}
