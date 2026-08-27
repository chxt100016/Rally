package com.rally.domain.tournament.matchmaking;

import java.util.List;

/** A legal manual or automatic group and the common facts available to it. */
public record MatchmakingGroup(
        List<Integer> entryNos,
        List<String> commonAvailableTimes,
        List<String> commonDistricts,
        String courtBookerId) {

    public MatchmakingGroup {
        entryNos = List.copyOf(entryNos);
        commonAvailableTimes = List.copyOf(commonAvailableTimes);
        commonDistricts = List.copyOf(commonDistricts);
    }
}
