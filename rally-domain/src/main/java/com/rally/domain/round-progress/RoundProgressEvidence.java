package com.rally.domain.tournament.roundprogress;

import com.rally.domain.tournament.enums.TournamentRoundEnum;

/** Completed-match evidence evaluated for one round. */
public record RoundProgressEvidence(
        TournamentRoundEnum round,
        int requiredCompletedCount,
        int actualCompletedCount) {
}
