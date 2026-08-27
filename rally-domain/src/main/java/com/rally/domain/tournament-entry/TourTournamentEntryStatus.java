package com.rally.domain.tour.tournamententry;

/** 签表参赛状态；退出状态不会被后续采集补丁恢复。 */
public enum TourTournamentEntryStatus {
    CONFIRMED,
    WITHDRAWN,
    RETIRED;

    public boolean isExited() {
        return this == WITHDRAWN || this == RETIRED;
    }
}
