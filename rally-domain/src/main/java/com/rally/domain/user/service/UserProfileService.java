package com.rally.domain.user.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.UserProfileGateway;
import com.rally.domain.user.model.ProfileChangeLogData;
import com.rally.domain.user.model.UserProfile;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 用户档案领域服务
 * 封装档案查询、初始化等业务逻辑
 */
@Service
public class UserProfileService {

    @Resource
    private UserProfileGateway userProfileGateway;

    @Resource
    private ProfileChangeLogGateway profileChangeLogGateway;

    /**
     * 查询用户档案，不存在则初始化 TBC
     */
    public void init(UserProfile profile) {
        if (ProfileStatusEnum.NONE != profile.getStatus()) {
            return;
        }

        profile.initializeTBC();
        userProfileGateway.save(profile);
    }

    /**
     * 查询用户档案，不自动初始化
     */
    public UserProfile getProfile(String userId) {
        UserProfile profile = userProfileGateway.findByUserId(userId);
        profile.assertExist();
        return profile;
    }

    /**
     * 保存用户档案
     */
    public void save(UserProfile profile) {
        userProfileGateway.save(profile);
    }

    /**
     * 获取核查期剩余场次
     */
    public Integer getReviewRemainingMatches(String userId) {
        Optional<ProfileChangeLogData> latestLog = profileChangeLogGateway.findLatestUnderReviewLog(userId);
        if (latestLog.isPresent() && latestLog.get().getAfterValue() != null) {
            return latestLog.get().getAfterValue().intValue();
        }
        return null;
    }
}
