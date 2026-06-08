package com.rally.domain.user.service;

import com.rally.domain.log.ProfileLogService;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserProfileGateway;
import com.rally.domain.user.model.UserProfile;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private ProfileLogService profileRecordService;

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
     * 批量查询用户档案（key = userId, value = UserProfile，过滤掉不存在的用户）
     * @param userIds 用户 ID 列表
     * @return userId → UserProfile 映射
     */
    public Map<String, UserProfile> listProfiles(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return userProfileGateway.findByUserIds(userIds).stream()
                .filter(p -> p != null && p.getUser() != null)
                .collect(Collectors.toMap(p -> p.getUser().getUserId(), p -> p, (a, b) -> a));
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
            profileRecordService.saveReviewTriggerLog(userProfile.getUser().getUserId(), requiredMatches);
        }

        // 3. 更新 NTRP
        userProfile.updateNtrpScore(newNtrp);
        tennisProfileGateway.update(userProfile.getProfile());

        // 4. 记录 NTRP 变更日志
        profileRecordService.saveNtrpChangeLog(userProfile.getUser().getUserId(), oldNtrp, newNtrp);
    }
}
