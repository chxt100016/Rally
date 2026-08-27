package com.rally.domain.tournament.roundprogress;

import com.rally.domain.tournament.enums.TournamentRoundEnum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Tournament and match facts captured in one consistent read snapshot. */
public record RoundProgressSnapshot(
        String tournamentId,
        Integer totalSlots,
        Integer currentFilledSlots,
        TournamentRoundEnum currentRound,
        List<RoundProgressMatchSnapshot> matches) {

    public RoundProgressSnapshot {
        matches = matches == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(matches));
    }
}
