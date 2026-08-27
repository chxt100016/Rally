package com.rally.domain.tournament.matchmaking;

import com.rally.domain.tournament.match.TournamentMatchRound;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Immutable input for one current-round matchmaking calculation. */
public record MatchmakingRequest(
        TournamentMatchRound round,
        int groupSize,
        List<MatchmakingCandidate> candidates,
        Set<Integer> excludedEntryNos,
        List<List<Integer>> manualGroups,
        Set<CompletedPairing> completedPairings) {

    public MatchmakingRequest {
        candidates = immutableListAllowingNulls(candidates);
        excludedEntryNos = immutableSetAllowingNulls(excludedEntryNos);
        manualGroups = immutableNestedListAllowingNulls(manualGroups);
        completedPairings = immutableSetAllowingNulls(completedPairings);
    }

    private static <T> List<T> immutableListAllowingNulls(List<T> source) {
        return source == null ? null : Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static <T> Set<T> immutableSetAllowingNulls(Set<T> source) {
        return source == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private static List<List<Integer>> immutableNestedListAllowingNulls(List<List<Integer>> source) {
        if (source == null) {
            return null;
        }
        List<List<Integer>> copy = new ArrayList<>(source.size());
        for (List<Integer> group : source) {
            copy.add(group == null ? null : Collections.unmodifiableList(new ArrayList<>(group)));
        }
        return Collections.unmodifiableList(copy);
    }
}
