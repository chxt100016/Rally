package com.rally.domain.user.gateway;

import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.user.model.ProfileChangeLogData;

import java.util.List;
import java.util.Optional;

/**
 * 用户档案变更日志网关
 */
public interface ProfileChangeLogGateway {

    /**
     * 保存变更日志
     */
    ProfileChangeLogData save(ProfileChangeLogData data);

    /**
     * 查询用户指定类型的变更日志
     */
    List<ProfileChangeLogData> findByUserIdAndType(String userId, ChangeLogTypeEnum type);

    /**
     * 查询用户最新的 under_review 日志（用于获取核查期进度）
     */
    Optional<ProfileChangeLogData> findLatestUnderReviewLog(String userId);
}
