package com.rally.domain.tournament.entry;

/** 建立后不可修改的报名身份。 */
public record TournamentEntryIdentity(
        String bizId,
        String tournamentId,
        String userId,
        int entryNo) {

    public TournamentEntryIdentity {
        bizId = requiredId(bizId, "报名业务 id");
        tournamentId = requiredId(tournamentId, "赛事 id");
        userId = requiredId(userId, "用户 id");
        if (entryNo <= 0) {
            throw new TournamentEntryDomainException(
                    TournamentEntry.TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                    "报名编号必须为正数");
        }
    }

    private static String requiredId(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new TournamentEntryDomainException(
                    TournamentEntry.TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                    label + "不能为空");
        }
        String normalized = value.strip();
        if (normalized.length() > 32) {
            throw new TournamentEntryDomainException(
                    TournamentEntry.TOURNAMENT_ENTRY_IDENTITY_CONFLICT,
                    label + "长度不能超过 32");
        }
        return normalized;
    }
}
