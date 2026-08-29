package com.rally.domain.tournament.match;

import java.util.List;

/** 两张比赛表的唯一写端口；根与全部参与者必须在同一事务中原子保存。 */
public interface TournamentMatchPersistence {

    TournamentMatchState findByBizId(String bizId);

    /**
     * C10 按自然键锁定读取最新根及全部参与者；首次不存在时返回 {@code null}。
     */
    TournamentMatchCancellationTarget findLatestByTournamentIdAndMatchNoForUpdate(
            String tournamentId,
            int matchNo);

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

    /**
     * C10 仅在根仍存在且状态不是 COMPLETED 时，同一事务物理删除根及全部参与者。
     */
    boolean deleteNotCompletedWithParticipants(String bizId);
}
