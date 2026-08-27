package com.rally.domain.tour.draw;

/**
 * {@code tour_draw} 的唯一领域写端口。
 *
 * <p>适配器必须区分自然键冲突；结构刷新按两列各自的非 null
 * 输入覆盖，null 保留原值。</p>
 */
public interface TourDrawPersistence {

    /** 按来源原始自然键读取，不存在时返回 {@code null}。 */
    TourDrawState findByIdentity(TourDrawIdentity identity);

    /** 插入一条当前来源状态。 */
    TourDrawInsertResult insert(TourDrawState state);

    /** 非 null 字段各自覆盖；返回 false 表示目标记录已经不存在。 */
    boolean refreshStructure(long id, Integer size, Integer totalRounds);
}
