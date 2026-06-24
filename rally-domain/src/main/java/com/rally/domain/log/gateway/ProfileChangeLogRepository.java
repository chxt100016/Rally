package com.rally.domain.log.gateway;

import com.rally.domain.log.model.ProfileChangeLogData;

import java.util.Optional;

/**
 * 用户档案变更日志网关
 */
public interface ProfileChangeLogRepository {

    /**
     * 保存变更日志
     */
    ProfileChangeLogData save(ProfileChangeLogData data);


    /**
     * 查询用户最新的 under_review 日志（用于获取核查期进度）
     */
    Optional<ProfileChangeLogData> findLatestUnderReviewLog(String userId);
}
