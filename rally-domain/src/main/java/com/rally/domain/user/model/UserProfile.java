package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.utils.Assert;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 用户档案聚合根
 * 组合 User + TennisProfile，只负责跨实体编排和判空，单实体规则下沉到对应实体内
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
     * 获取档案状态，无档案视为 NONE
     */
    public ProfileStatusEnum getStatus() {
        if (profile == null) {
            return ProfileStatusEnum.NONE;
        }
        return profile.getStatus();
    }

    public boolean hasProfile() {
        return this.getStatus() != ProfileStatusEnum.TBC && this.getStatus() != ProfileStatusEnum.NONE;
    }

    /**
     * 是否在核查期
     */
    public boolean isUnderReview() {
        return profile != null && profile.underReview();
    }

    /**
     * 校验 NTRP 冷却期，冷却中抛业务异常
     */
    public void assertNtrpCooldown() {
        if (profile == null) {
            return;
        }
        profile.assertNtrpCooldown();
    }

    /**
     * 校验 NTRP 是否触发核查期，触发则更新档案状态
     * @return 触发时返回 requiredMatches，未触发返回 -1
     */
    public int triggerReviewIfNeeded(BigDecimal newNtrp) {
        return profile.triggerReviewIfNeeded(newNtrp);
    }

    /**
     * 更新 NTRP 分值和时间
     */
    public void updateNtrpScore(BigDecimal newNtrp) {
        profile.updateNtrpScore(newNtrp);
    }

    /**
     * 计算自评修改剩余冷却天数
     * 可编辑时返回 null，冷却中返回剩余天数
     */
    public Integer calculateNtrpCooldownDays() {
        if (profile == null) {
            return null;
        }
        return profile.ntrpCooldownRemainingDays();
    }

    /**
     * 初始化 TBC 档案（首次访问时）
     */
    public void initializeTBC() {
        if (profile == null) {
            profile = new TennisProfileData();
        }
        profile.initTBC(user.getUserId());
    }

    /**
     * 完成 onboarding：用户基本信息 + 档案分别由各自实体落值
     */
    public void completeOnboarding(OnboardingCmd cmd) {
        profile.completeOnboarding(cmd.getNtrpScore(), cmd.getVideos());
    }

    /**
     * 追加一条视频
     */
    public void addVideo(VideoVO video) {
        profile.addVideo(video);
    }

    /**
     * 删除一条视频
     */
    public void deleteVideo(String key) {
        profile.deleteVideo(key);
    }

    /**
     * 修改视频标题
     */
    public void updateVideo(String key, String title) {
        profile.updateVideo(key, title);
    }


    public void assertCompleted() {
        boolean basicDefault = user.isBasicDefault();
        boolean profileIncomplete = !hasProfile();

        if (basicDefault && profileIncomplete) {
            throw new BusinessException(BizErrorCode.REGISTRATION_INCOMPLETE);
        }
        if (basicDefault) {
            throw new BusinessException(BizErrorCode.USER_INCOMPLETE);
        }
        Assert.isTrue(!profileIncomplete, BizErrorCode.ONBOARDING_INCOMPLETE);
    }

    public String getUserId() {
        return this.getUser().getUserId();
    }
}
