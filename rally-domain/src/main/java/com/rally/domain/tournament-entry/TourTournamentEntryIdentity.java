package com.rally.domain.tour.tournamententry;

/** 由签表内部 id 与来源球员编号组成的不可变参赛身份。 */
public record TourTournamentEntryIdentity(long drawId, String playerId) {

    public static TourTournamentEntryIdentity of(Long drawId, String playerId) {
        require(drawId != null && drawId > 0, "签表 id 必须为正数");
        require(playerId != null && !playerId.isBlank(), "球员编号不能为空");
        require(playerId.length() <= 50, "球员编号长度不能超过 50");
        return new TourTournamentEntryIdentity(drawId, playerId);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourTournamentEntryDomainException(
                    TourTournamentEntry.TOUR_ENTRY_IDENTITY_CONFLICT, message);
        }
    }
}
