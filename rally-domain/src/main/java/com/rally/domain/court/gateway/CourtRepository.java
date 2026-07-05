package com.rally.domain.court.gateway;

import com.rally.domain.court.model.CourtData;

import java.util.List;
import java.util.Map;

/**
 * 球场网关接口
 */
public interface CourtRepository {

    /**
     * 保存球场（新增或更新）
     */
    void save(CourtData data);

    /**
     * 根据 bizId 查询
     */
    CourtData findByBizId(String bizId);

    /**
     * 查询城市下所有球场
     */
    List<CourtData> findByCityCode(String cityCode);

    /**
     * 模糊搜索球场名称
     */
    List<CourtData> fuzzySearchByName(String cityCode, String keyword);

    /**
     * 批量增加球场约球次数
     * @param courtIdCountMap 球场 bizId -> 增加次数
     */
    void batchIncrementMeetupCount(Map<String, Integer> courtIdCountMap);
}
