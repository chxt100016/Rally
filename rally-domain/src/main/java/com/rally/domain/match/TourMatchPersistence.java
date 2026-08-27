package com.rally.domain.tour.match;

/**
 * {@code tour_match} 的唯一领域写端口。
 *
 * <p>适配器必须使用 {@code draw_id+match_id} 自然键查找与收敛并发插入；
 * {@link #replaceSnapshot(TourMatchState)} 必须用聚合产出的完整合并状态原子更新。</p>
 */
public interface TourMatchPersistence {

    /** 按自然键读取，不存在时返回 {@code null}。 */
    TourMatchState findByIdentity(TourMatchIdentity identity);

    /** 插入一条已校验的新快照。 */
    TourMatchInsertResult insert(TourMatchState state);

    /** 按内部 id 原子替换完整快照；false 表示目标已不存在。 */
    boolean replaceSnapshot(TourMatchState state);
}
