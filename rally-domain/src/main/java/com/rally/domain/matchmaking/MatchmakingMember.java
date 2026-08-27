package com.rally.domain.tournament.matchmaking;

/** A member snapshot used only by the pure matchmaking calculation. */
public record MatchmakingMember(String userId, boolean canBookCourt, String gender) {
}
