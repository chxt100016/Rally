package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.enums.ProfileStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;
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
        profile.setCityCode(cmd.getCityCode());
        profile.setVideoUrls(cmd.getVideoKeys());
        profile.setStatus(ProfileStatusEnum.NORMAL);
        profile.setNtrpUpdatedAt(LocalDateTime.now());
    }
}
