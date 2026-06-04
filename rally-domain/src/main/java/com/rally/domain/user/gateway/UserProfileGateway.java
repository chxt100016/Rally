package com.rally.domain.user.gateway;

import com.rally.domain.user.model.UserProfile;

/**
 * 用户档案聚合网关
 */
public interface UserProfileGateway {

    /**
     * 查询用户档案聚合，user 或 profile 可为 null
     */
    UserProfile findByUserId(String userId);

    /**
     * 保存用户档案聚合（user + profile）
     */
    void save(UserProfile userProfile);
}
