package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.model.NearbyResult;

import java.util.List;
import java.util.Set;

/**
 * 距离排序网关接口（Redis GEO 抽象）
 */
public interface NearbyRepository {
    /**
     * 写入/更新一个约球的地理位置（发布、改场地时调用）
     */
    void add(String cityCode, String meetupId, double lng, double lat);

    /**
     * 失效清理（约球进入 CLOSED/FINISHED 时调用）
     */
    void remove(String cityCode, String meetupId);

    /**
     * 按半径升序检索，返回 meetupId + 距离（米），已按距离 ASC
     */
    List<NearbyResult> searchByRadius(String cityCode, double lng, double lat, double radiusMeters);

    /**
     * 按距离升序检索城市所有活动（不限半径），返回 meetupId + 距离（米）
     */
    List<NearbyResult> searchAllByDistance(String cityCode, double lng, double lat);

    /**
     * 一致性校验用：列出某城市 GEO 集合内全部 meetupId
     */
    Set<String> members(String cityCode);
}
