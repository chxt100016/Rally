package com.rally.domain.tour.tournament;

import java.util.Locale;

/** 可由后续来源纠正的职业赛事主档状态。 */
public enum TourTournamentStatus {
    ACTIVE,
    COMPLETED;

    public static TourTournamentStatus fromSource(String value) {
        if (value == null || value.isBlank()) {
            throw invalid();
        }
        try {
            return valueOf(value.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw invalid();
        }
    }

    /** 与 tour_tournament.status 现有小写存储约定一致。 */
    public String databaseValue() {
        return name().toLowerCase(Locale.ROOT);
    }

    private static TourTournamentDomainException invalid() {
        return new TourTournamentDomainException(
                TourTournament.TOUR_TOURNAMENT_PROFILE_INVALID,
                "赛事状态只能是 ACTIVE 或 COMPLETED");
    }
}
