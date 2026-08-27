package com.rally.domain.tour.tournament;

/** 由来源赛事编号和年份组成的不可变职业赛事年度身份；tour 不参与身份。 */
public record TourTournamentIdentity(String tournamentId, int year) {

    private static final int TOURNAMENT_ID_MAX_LENGTH = 50;

    public static TourTournamentIdentity fromSource(String tournamentId, Integer year) {
        require(tournamentId != null && !tournamentId.isBlank(),
                "来源赛事编号不能为空");
        String normalizedTournamentId = tournamentId.strip();
        require(normalizedTournamentId.length() <= TOURNAMENT_ID_MAX_LENGTH,
                "来源赛事编号长度不能超过 50");
        require(year != null && year > 0,
                "赛事年份必须为正数");
        return new TourTournamentIdentity(normalizedTournamentId, year);
    }

    public TourTournamentIdentity {
        require(tournamentId != null && !tournamentId.isBlank(),
                "来源赛事编号不能为空");
        require(tournamentId.equals(tournamentId.strip())
                        && tournamentId.length() <= TOURNAMENT_ID_MAX_LENGTH,
                "来源赛事编号格式非法");
        require(year > 0, "赛事年份必须为正数");
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourTournamentDomainException(
                    TourTournament.TOUR_TOURNAMENT_IDENTITY_CONFLICT, message);
        }
    }
}
