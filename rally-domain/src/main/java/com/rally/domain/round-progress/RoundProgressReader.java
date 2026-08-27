package com.rally.domain.tournament.roundprogress;

/**
 * Read-only port for round-progress evaluation.
 *
 * <p>The adapter must load the tournament row and its match projections from
 * one consistent database snapshot. A {@code null} result means that the
 * tournament does not exist.</p>
 */
public interface RoundProgressReader {

    RoundProgressSnapshot loadSnapshot(String tournamentId);
}
