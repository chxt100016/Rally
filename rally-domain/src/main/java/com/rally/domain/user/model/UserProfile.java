package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.enums.ProfileStatusEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

/**
 * 用户档案聚合根
 * 组合 User + TennisProfile，封装用户档案相关业务规则
 */
@Data
public class UserProfile {

    private UserData user;
    private TennisProfileData profile;

    private UserProfile() {}

    /**
     * 静态工厂：组合 User + Profile
     */
    public static UserProfile create(UserData user, TennisProfileData profile) {
        UserProfile instance = new UserProfile();
        instance.user = user;
        instance.profile = profile;
        return instance;
    }

    public void assertExist() {
        if (user == null) {
            throw new BusinessException(BizErrorCode.TOKEN_INVALID);
        }
    }

    /**
     * 获取档案状态
     */
    public ProfileStatusEnum getStatus() {
        if (profile == null) {
            return ProfileStatusEnum.NONE;
        }
        return profile.getStatus();
    }

    /**
     * 是否在核查期
     */
    public boolean isUnderReview() {
        return profile != null && Boolean.TRUE.equals(profile.getIsUnderReview());
    }

    /**
     * 计算 NTRP 冷却天数
     * 根据可信度返回对应的冷却天数
     */
    public int calculateNtrpCooldownDays(int lowDays, int midDays, int highDays) {
        BigDecimal credibilityScore = profile != null ? profile.getCredibilityScore() : null;
        if (credibilityScore == null) {
            return lowDays;
        }
        float credibility = credibilityScore.floatValue();
        if (credibility < 30) {
            return lowDays;
        } else if (credibility < 60) {
            return midDays;
        } else {
            return highDays;
        }
    }

    /**
     * 计算 NTRP 可编辑状态和冷却剩余天数
     *
     * @param cooldownDays 冷却总天数
     * @return [isEditable, cooldownRemainingDays]
     */
    public Object[] calculateNtrpEditableStatus(int cooldownDays) {
        if (profile == null || profile.getNtrpUpdatedAt() == null) {
            return new Object[]{true, null};
        }

        long daysSinceUpdate = ChronoUnit.DAYS.between(profile.getNtrpUpdatedAt(), LocalDateTime.now());
        if (daysSinceUpdate < cooldownDays) {
            return new Object[]{false, (int) (cooldownDays - daysSinceUpdate)};
        }
        return new Object[]{true, null};
    }

    /**
     * 计算自评修改剩余冷却天数
     * 可编辑时返回 null，不可编辑时返回剩余天数
     */
    public Integer calculateNtrpLockDays() {
        if (profile == null || profile.getNtrpUpdatedAt() == null) {
            return null;
        }
        int lowDays = SystemConfig.getInt("score.ntrp.cooldown_low_days", 30);
        int midDays = SystemConfig.getInt("score.ntrp.cooldown_mid_days", 60);
        int highDays = SystemConfig.getInt("score.ntrp.cooldown_high_days", 90);
        int cooldown = calculateNtrpCooldownDays(lowDays, midDays, highDays);
        Object[] editStatus = calculateNtrpEditableStatus(cooldown);
        if (!Boolean.TRUE.equals(editStatus[0]) && editStatus[1] != null) {
            return (Integer) editStatus[1];
        }
        return null;
    }

    /**
     * 初始化 TBC 档案（首次访问时）
     */
    public void initializeTBC() {
        if (profile == null) {
            profile = new TennisProfileData();
        }
        profile.setUserId(user.getUserId());
        profile.setStatus(ProfileStatusEnum.TBC);
        profile.setVideoUrls(new ArrayList<>());
    }

    /**
     * 完成 onboarding，设置状态为 NORMAL
     */
    public void completeOnboarding(OnboardingCmd cmd) {
        // 更新用户基本信息
        if (cmd.getGender() != null) {
            try {
                user.setGender(GenderEnum.valueOf(cmd.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "性别值非法");
            }
        }
        if (cmd.getBirthday() != null) {
            user.setBirthday(cmd.getBirthday());
        }

        // 更新档案
        profile.setNtrpScore(cmd.getNtrpScore());
        user.setCityCode(cmd.getCityCode());
        profile.setVideoUrls(cmd.getVideoKeys());
        profile.setStatus(ProfileStatusEnum.NORMAL);
        profile.setNtrpUpdatedAt(LocalDateTime.now());
    }
}
