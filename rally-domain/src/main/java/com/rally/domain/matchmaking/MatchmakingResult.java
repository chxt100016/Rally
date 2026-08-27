package com.rally.domain.tournament.matchmaking;

import java.util.List;

/** Explicit accepted or rejected outcome; normal no-match results are accepted. */
public final class MatchmakingResult {

    private final MatchmakingRejection rejection;
    private final List<MatchmakingGroup> groups;
    private final List<Integer> unmatchedEntryNos;

    private MatchmakingResult(
            MatchmakingRejection rejection,
            List<MatchmakingGroup> groups,
            List<Integer> unmatchedEntryNos) {
        this.rejection = rejection;
        this.groups = List.copyOf(groups);
        this.unmatchedEntryNos = List.copyOf(unmatchedEntryNos);
    }

    public static MatchmakingResult accepted(
            List<MatchmakingGroup> groups,
            List<Integer> unmatchedEntryNos) {
        return new MatchmakingResult(null, groups, unmatchedEntryNos);
    }

    public static MatchmakingResult rejected(MatchmakingRejection rejection) {
        return new MatchmakingResult(rejection, List.of(), List.of());
    }

    public boolean isAccepted() {
        return rejection == null;
    }

    public MatchmakingRejection getRejection() {
        return rejection;
    }

    public List<MatchmakingGroup> getGroups() {
        return groups;
    }

    public List<Integer> getUnmatchedEntryNos() {
        return unmatchedEntryNos;
    }
}
