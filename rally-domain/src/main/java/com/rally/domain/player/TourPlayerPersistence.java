package com.rally.domain.tour.player;

/**
 * {@code tour_player} 的唯一领域写端口。
 *
 * <p>适配器必须始终使用 {@code tour+player_id} 查询，并将一批 C1 命令放在同一事务中；
 * 更新只能写 {@link TourPlayerProfilePatch} 中的非 null 字段。</p>
 */
public interface TourPlayerPersistence {

    /** 按规范复合自然键读取，不存在时返回 {@code null}。 */
    TourPlayerState findByIdentity(TourPlayerIdentity identity);

    /** 插入一条已校验的新球员资料。 */
    TourPlayerInsertResult insert(TourPlayerState state);

    /** 原子合并非空资料补丁；false 表示目标记录已经不存在。 */
    boolean applyNonNullPatch(long id, TourPlayerProfilePatch patch);
}
