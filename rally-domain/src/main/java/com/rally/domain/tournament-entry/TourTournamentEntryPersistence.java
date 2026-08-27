package com.rally.domain.tour.tournamententry;

/**
 * {@code tour_tournament_entry} 的唯一领域写端口。
 *
 * <p>适配器必须按 {@code draw_id+player_id} 查询；同批 C1 命令按到达顺序放在同一事务中。
 * 资格更新只能写补丁中的非 null 列，状态更新只能由 C2 调用。</p>
 */
public interface TourTournamentEntryPersistence {

    /** 按复合自然键读取，不存在时返回 {@code null}。 */
    TourTournamentEntryState findByIdentity(TourTournamentEntryIdentity identity);

    /** 插入一条已校验的新参赛项。 */
    TourTournamentEntryInsertResult insert(TourTournamentEntryState state);

    /** 原子合并非空资格补丁；false 表示目标记录已不存在。 */
    boolean applyNonNullQualificationPatch(
            long id, TourTournamentEntryQualificationPatch patch);

    /** 原子写入明确退出状态；false 表示目标记录已不存在。 */
    boolean updateStatus(long id, TourTournamentEntryStatus targetStatus);
}
