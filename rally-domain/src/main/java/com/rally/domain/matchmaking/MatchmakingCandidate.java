package com.rally.domain.tournament.matchmaking;

import com.rally.domain.tournament.entry.TournamentEntryStatus;
import com.rally.domain.tournament.match.TournamentMatchRound;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * An immutable entry-unit snapshot. A singles entry normally has one member and
 * a doubles entry two; {@code requiredMemberCount} lets the service reject an
 * incomplete unit without consulting persistence.
 */
public record MatchmakingCandidate(
        Integer entryNo,
        TournamentMatchRound round,
        TournamentEntryStatus status,
        List<MatchmakingMember> members,
        int requiredMemberCount,
        Set<String> commonAvailableTimes,
        Set<String> commonDistricts,
        LocalDateTime joinedTime) {

    public MatchmakingCandidate {
        members = immutableListAllowingNulls(members);
        commonAvailableTimes = immutableSetAllowingNulls(commonAvailableTimes);
        commonDistricts = immutableSetAllowingNulls(commonDistricts);
    }

    private static <T> List<T> immutableListAllowingNulls(List<T> source) {
        return source == null ? null : Collections.unmodifiableList(new ArrayList<>(source));
    }

    private static <T> Set<T> immutableSetAllowingNulls(Set<T> source) {
        return source == null ? null : Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }
}
