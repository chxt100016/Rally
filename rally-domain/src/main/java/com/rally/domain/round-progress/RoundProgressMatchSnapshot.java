package com.rally.domain.tournament.roundprogress;

import com.rally.domain.tournament.enums.TournamentRoundEnum;

/** Read-only projection of the round and persisted status of one tournament match. */
public record RoundProgressMatchSnapshot(
        String tournamentId,
        TournamentRoundEnum round,
        String status) {
}
