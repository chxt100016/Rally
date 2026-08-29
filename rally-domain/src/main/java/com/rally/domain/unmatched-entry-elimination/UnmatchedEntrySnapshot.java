package com.rally.domain.tournament.unmatchedentryelimination;

import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;

/** Read-only entry facts used to decide whether an entire entry may be eliminated. */
public record UnmatchedEntrySnapshot(
        Integer entryNo,
        String userId,
        String partnerId,
        TournamentEntryStatusEnum status,
        TournamentRoundEnum currentRound) {
}
