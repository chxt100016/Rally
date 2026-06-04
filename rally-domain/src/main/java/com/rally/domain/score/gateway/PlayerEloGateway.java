package com.rally.domain.score.gateway;

import com.rally.domain.score.model.EloResult;

import java.util.List;

/**
 * ELO 聚合表读写网关接口
 */
public interface PlayerEloGateway {

    /**
     * 获取用户当前 ELO 分（不存在则返回初始值）
     */
    float getEloScore(String userId);

    /**
     * 批量更新 ELO 分（upsert：存在则更新，不存在则初始化）
     */
    void batchUpsert(List<EloResult> results);
}
