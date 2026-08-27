package com.rally.domain.tournament.matchmaking;

/** Two entry numbers that have already appeared in the same completed match. */
public record CompletedPairing(Integer leftEntryNo, Integer rightEntryNo) {
}
