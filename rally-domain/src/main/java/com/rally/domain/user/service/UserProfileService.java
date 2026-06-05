package com.rally.domain.user.service;

import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.TennisProfileGateway;
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
    private TennisProfileGateway tennisProfileGateway;

    @Resource
    private ProfileChangeLogGateway profileChangeLogGateway;

    @Resource
    private ProfileChangeLogService profileChangeLogService;

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
     * 自评修改：校验冷却 → 触发核查期 → 更新分值 → 记录日志
     */
    public void updateNtrp(UserProfile userProfile, BigDecimal newNtrp) {
        // 1. 冷却校验
        userProfile.assertNtrpCooldown();

        BigDecimal oldNtrp = userProfile.getProfile().getNtrpScore();

        // 2. 检查是否触发核查期
        int requiredMatches = userProfile.triggerReviewIfNeeded(newNtrp);
        if (requiredMatches > 0) {
            profileChangeLogService.saveReviewTriggerLog(userProfile.getUser().getUserId(), requiredMatches);
        }

        // 3. 更新 NTRP
        userProfile.updateNtrpScore(newNtrp);
        tennisProfileGateway.update(userProfile.getProfile());

        // 4. 记录 NTRP 变更日志
        profileChangeLogService.saveNtrpChangeLog(userProfile.getUser().getUserId(), oldNtrp, newNtrp);
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
