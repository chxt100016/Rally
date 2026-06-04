package com.rally.domain.user.gateway;

import com.rally.domain.user.model.TennisProfileData;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * 球员档案表读写网关
 */
public interface TennisProfileGateway {

    /**
     * 保存档案
     */
    TennisProfileData save(TennisProfileData data);

    /**
     * 根据用户 ID 查询档案
     */
    Optional<TennisProfileData> findByUserId(String userId);

    /**
     * 更新档案（非 null 字段）
     */
    TennisProfileData update(TennisProfileData data);

    /**
     * 更新视频列表
     */
    void updateVideoUrls(String userId, List<String> videoUrls);

    /**
     * 更新评分字段（由评分域调用）
     */
    void updateScoreFields(String userId, BigDecimal reputationScore, BigDecimal credibilityScore,
                           BigDecimal calibrationScore, Boolean isNewbie);
}
