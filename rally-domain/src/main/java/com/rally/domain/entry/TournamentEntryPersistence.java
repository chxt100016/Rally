package com.rally.domain.tournament.entry;

/** rally_tournament_entry 的写端口；通用保存不带版本条件，C11 单独使用条件更新。 */
public interface TournamentEntryPersistence {

    TournamentEntryState findByTournamentAndUser(String tournamentId, String userId);

    /** 在当前事务中按赛事与用户自然键锁定报名。 */
    TournamentEntryState findByTournamentAndUserForUpdate(
            String tournamentId,
            String userId);

    TournamentEntryInsertResult insert(TournamentEntryState state);

    boolean saveByBizId(TournamentEntryState state);

    /**
     * C11 条件更新：仅当 biz_id 存在、current_round 等于预期轮次，
     * 且当前状态仍为 WAITING/FROZEN 时保存 ELIMINATED 候选状态。
     *
     * @return 是否唯一记录被条件更新
     */
    boolean eliminateUnmatchedByBizId(
            TournamentEntryState state,
            TournamentEntryRound expectedCurrentRound);
}
