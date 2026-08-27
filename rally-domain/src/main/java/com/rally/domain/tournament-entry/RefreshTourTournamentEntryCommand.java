package com.rally.domain.tour.tournamententry;

/** C1：新增参赛项，或按到达顺序用非空来源字段刷新资格。 */
public record RefreshTourTournamentEntryCommand(
        Long drawId,
        String playerId,
        Short seed,
        String entryType) {
}
