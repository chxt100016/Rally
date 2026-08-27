package com.rally.domain.tournament.entry;

/** rally_tournament_entry 的写端口；更新按 biz_id 普通保存，不带版本或原状态条件。 */
public interface TournamentEntryPersistence {

    TournamentEntryState findByTournamentAndUser(String tournamentId, String userId);

    TournamentEntryInsertResult insert(TournamentEntryState state);

    boolean saveByBizId(TournamentEntryState state);
}
