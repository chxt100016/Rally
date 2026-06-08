package com.rally.domain.user.gateway;

import com.rally.domain.user.model.UserProfile;

import java.util.List;

/**
 * 用户档案聚合网关
 */
public interface UserProfileGateway {

    /**
     * 查询用户档案聚合，user 或 profile 可为 null
     */
    UserProfile findByUserId(String userId);

    /**
     * 批量查询用户档案聚合
     * @param userIds 用户 ID 列表
     * @return 用户档案列表（与 userIds 顺序一致，不存在的用户对应 null）
     */
    List<UserProfile> findByUserIds(List<String> userIds);

    /**
     * 保存用户档案聚合（user + profile）
     */
    void save(UserProfile userProfile);
}
