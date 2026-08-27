package com.rally.domain.tournament.match;

import java.util.List;

/** 两张比赛表的唯一写端口；根与全部参与者必须在同一事务中原子保存。 */
public interface TournamentMatchPersistence {

    TournamentMatchState findByBizId(String bizId);

    TournamentMatchInsertResult insert(
            TournamentMatchState state,
            List<TournamentMatchParticipant> participants);

    /** 根按 expectedVersion 条件更新成功后，同事务整组保存参与者。 */
    boolean replaceWithVersion(
            TournamentMatchState state,
            List<TournamentMatchParticipant> participants,
            int expectedVersion);

    /** 仅在状态仍为 MATCHED/BOOKING 且版本相符时删除根及全部参与者。 */
    boolean deleteUnsubmittedWithVersion(String bizId, int expectedVersion);
}
