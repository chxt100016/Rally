package com.rally.domain.court.cache;

import com.rally.domain.court.model.CourtData;

/**
 * 球场内存缓存（按 bizId），用于卡片底图等高频回查场景。
 */
public interface CourtCache {

    /**
     * 按 bizId 获取球场，缺失时回源 DB；无此球场返回 null。
     */
    CourtData getByBizId(String bizId);

    /**
     * 失效指定 bizId 缓存（球场信息变更后调用）。
     */
    void invalidate(String bizId);
}
