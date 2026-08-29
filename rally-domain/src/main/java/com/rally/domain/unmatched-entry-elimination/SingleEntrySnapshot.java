package com.rally.domain.tournament.unmatchedentryelimination;

import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;

/** Minimal read-only facts of the single target entry. */
public record SingleEntrySnapshot(
        String userId,
        TournamentEntryStatusEnum status,
        TournamentRoundEnum currentRound) {
}
