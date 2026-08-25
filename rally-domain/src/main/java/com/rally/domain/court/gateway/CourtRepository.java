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
     * 按三方来源编号批量查询已收录的球场
     */
    List<CourtData> findBySourceIds(List<String> sourceIds);

    /**
     * 批量保存球场（按 bizId 判定新增还是更新）
     *
     * @return 实际写入成功的条数
     */
    int batchSave(List<CourtData> courts);

    /**
     * 批量增加球场约球次数
     * @param courtIdCountMap 球场 bizId -> 增加次数
     */
    void batchIncrementMeetupCount(Map<String, Integer> courtIdCountMap);
}
