package com.rally.domain.user.model;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.user.enums.GenderEnum;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.utils.Assert;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 用户档案聚合根
 * 组合 User + TennisProfile，只负责跨实体编排和判空，单实体规则下沉到对应实体内
 */
@Data
public class UserProfile {

    public static final String USER_IDENTITY_INVALID = "USER_IDENTITY_INVALID";
    public static final String USER_PROFILE_OWNERSHIP_CONFLICT = "USER_PROFILE_OWNERSHIP_CONFLICT";
    public static final String USER_REVIEW_STATE_INCONSISTENT = "USER_REVIEW_STATE_INCONSISTENT";
    public static final String USER_PROFILE_INCOMPLETE = "USER_PROFILE_INCOMPLETE";
    public static final String USER_VIDEO_INVALID = "USER_VIDEO_INVALID";
    public static final String USER_NTRP_COOLDOWN = "USER_NTRP_COOLDOWN";

    private static final int NICKNAME_MAX_LENGTH = 64;
    private static final int AVATAR_URL_MAX_LENGTH = 512;
    private static final int PHONE_MAX_LENGTH = 20;

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

    /** C1 建立默认用户；编号生成和跨聚合唯一性最终由仓储及数据库唯一键保证。 */
    public static UserProfile establishDefault(
            String defaultNickname,
            String defaultAvatarUrl,
            Supplier<String> userIdGenerator) {
        require(userIdGenerator != null, USER_IDENTITY_INVALID, "用户编号生成器不能为空");
        requireNotBlank(defaultNickname, USER_IDENTITY_INVALID, "默认昵称不能为空");
        requireNotBlank(defaultAvatarUrl, USER_IDENTITY_INVALID, "默认头像资源键不能为空");
        requireLength(defaultNickname, NICKNAME_MAX_LENGTH, USER_IDENTITY_INVALID, "默认昵称超过存储上限");
        requireLength(defaultAvatarUrl, AVATAR_URL_MAX_LENGTH, USER_IDENTITY_INVALID, "默认头像资源键超过存储上限");

        UserData user = new UserData();
        user.setUserId(userIdGenerator.get());
        user.setNickname(defaultNickname);
        user.setAvatarUrl(defaultAvatarUrl);
        user.setGender(GenderEnum.UNDISCLOSED);
        UserProfile aggregate = create(user, null);
        aggregate.checkIdentityInvariant();
        return aggregate;
    }

    /** 将并发建立用户时的 {@code uk_user_id} 冲突转换为 I1 的稳定错误。 */
    public static UserDomainException identityConflict(Throwable cause) {
        return new UserDomainException(USER_IDENTITY_INVALID, "用户编号已存在", cause);
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

    /** 查询基础资料与网球档案是否均已完成，不改变聚合状态。 */
    public boolean isComplete() {
        return user != null && !user.isBasicDefault() && hasProfile();
    }

    /** C2 按非空字段修改基础资料；空字符串是已提交值，列容量由数据库约束。 */
    public void updateBasicProfile(EditProfileCmd command) {
        require(user != null, USER_IDENTITY_INVALID, "用户不存在");
        require(command != null, USER_IDENTITY_INVALID, "基础资料修改命令不能为空");

        if (command.getNickname() != null) {
            user.setNickname(command.getNickname());
        }
        if (command.getAvatarUrl() != null) {
            user.setAvatarUrl(command.getAvatarUrl());
        }
        if (command.getGender() != null) {
            user.setGender(command.getGender());
        }
        if (command.getBirthday() != null) {
            user.setBirthday(command.getBirthday());
        }
        if (command.getCityCode() != null) {
            user.setCityCode(command.getCityCode());
        }
        if (command.getBio() != null) {
            user.setBio(command.getBio());
        }
        checkIdentityInvariant();
    }

    /** C3 覆盖绑定非空授权手机号；手机号不承担跨用户唯一身份语义。 */
    public void bindAuthorizedPhone(String phone) {
        require(user != null, USER_IDENTITY_INVALID, "用户不存在");
        requireNotBlank(phone, USER_IDENTITY_INVALID, "授权手机号不能为空");
        requireLength(phone, PHONE_MAX_LENGTH, USER_IDENTITY_INVALID, "授权手机号超过存储上限");
        user.setPhone(phone);
        checkIdentityInvariant();
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
        require(user != null, USER_IDENTITY_INVALID, "用户不存在");
        if (profile != null) {
            return;
        }
        profile = new TennisProfileData();
        profile.initTBC(user.getUserId());
        checkProfileInvariants(false);
    }

    /** C4 初始化待完善档案，并由调用方提供本次生成的雪花业务编号和初始评分。 */
    public void initializeTBC(
            Supplier<String> profileIdGenerator,
            Integer initialReputation,
            Integer initialCredibility,
            Integer initialCalibration) {
        if (profile != null) {
            return;
        }
        require(profileIdGenerator != null, USER_PROFILE_OWNERSHIP_CONFLICT, "档案编号生成器不能为空");
        require(initialReputation != null && initialCredibility != null && initialCalibration != null,
                USER_PROFILE_INCOMPLETE, "档案初始评分配置不能为空");
        String generatedProfileId = profileIdGenerator.get();
        requireNotBlank(generatedProfileId, USER_PROFILE_OWNERSHIP_CONFLICT, "档案业务编号不能为空");
        initializeTBC();
        profile.setBizId(generatedProfileId);
        profile.setReputationScore(initialReputation);
        profile.setCredibilityScore(initialCredibility);
        profile.setCalibrationScore(initialCalibration);
        checkProfileInvariants(false);
    }

    /**
     * 完成 onboarding：用户基本信息 + 档案分别由各自实体落值
     */
    public void completeOnboarding(OnboardingCmd cmd) {
        profile.completeOnboarding(cmd.getNtrpScore(), cmd.getVideos());
    }

    /**
     * C5 从 NONE 或 TBC 完成初始档案。NONE 时同一命令内建立实体；初始评分由应用层配置传入。
     */
    public void completeInitialProfile(
            BigDecimal ntrpScore,
            List<VideoVO> videos,
            Integer initialReputation,
            Integer initialCredibility,
            Integer initialCalibration,
            Supplier<String> profileIdGenerator,
            LocalDateTime changedAt) {
        require(user != null, USER_IDENTITY_INVALID, "用户不存在");
        String generatedProfileId = null;
        if (profile == null) {
            generatedProfileId = profileIdGenerator.get();
            profile = new TennisProfileData();
            profile.setBizId(generatedProfileId);
            profile.setUserId(user.getUserId());
        }
        profile.setNtrpScore(ntrpScore);
        profile.setVideos(videos);
        profile.setStatus(ProfileStatusEnum.NORMAL);
        profile.setReputationScore(initialReputation);
        profile.setCredibilityScore(initialCredibility);
        profile.setCalibrationScore(initialCalibration);
        checkProfileInvariants(false);
    }

    /**
     * 追加一条视频
     */
    public void addVideo(VideoVO video) {
        profile.addVideo(video);
    }

    /** C6 兼容旧入口；main 不在领域层执行数量校验。 */
    public void addVideo(VideoVO video, int interactionMaxCount) {
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

    /**
     * C9 修改自评 NTRP。返回本次是否触发核查以及核查所需场次；-1 表示未触发新的核查。
     */
    public int changeSelfRatedNtrp(
            BigDecimal newNtrp,
            LocalDateTime now,
            int cooldownDays,
            BigDecimal reviewTriggerDelta,
            int requiredReviewMatches) {
        profile.assertNtrpCooldown();
        int triggeredMatches = profile.triggerReviewIfNeeded(newNtrp);
        profile.updateNtrpScore(newNtrp);
        return triggeredMatches;
    }

    public GenderEnum getGender() {
        if (Objects.isNull(user)) {
            return null;
        }
        return user.getGender();
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

    /** I1-I6：供仓储保存前复核完整聚合状态。 */
    public void checkInvariants() {
        checkIdentityInvariant();
        checkProfileInvariants(false);
    }

    private void checkIdentityInvariant() {
        require(user != null, USER_IDENTITY_INVALID, "用户不能为空");
        requireNotBlank(user.getUserId(), USER_IDENTITY_INVALID, "用户编号不能为空");
        require(user.getGender() != null, USER_IDENTITY_INVALID, "用户性别非法");
    }

    private void checkProfileInvariants(boolean requireCompleteContent) {
        checkIdentityInvariant();
        if (profile == null) {
            return;
        }
        require(Objects.equals(user.getUserId(), profile.getUserId()),
                USER_PROFILE_OWNERSHIP_CONFLICT, "网球档案不属于当前用户");
    }

    private static void requireNotBlank(String value, String errorIdentifier, String message) {
        require(value != null && !value.trim().isEmpty(), errorIdentifier, message);
    }

    private static void requireLength(String value, int maxLength, String errorIdentifier, String message) {
        require(value == null || value.length() <= maxLength, errorIdentifier, message);
    }

    private static void require(boolean condition, String errorIdentifier, String message) {
        if (!condition) {
            throw new UserDomainException(errorIdentifier, message);
        }
    }
}
