package com.rally.domain.tour.tournamententry;

/** C2：记录调用方已经确认的明确退出意图。 */
public record RecordTourTournamentEntryExitCommand(
        TourTournamentEntryStatus targetStatus,
        boolean explicitExitIntent) {
}
