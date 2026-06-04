package com.rally.user;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.user.enums.ChangeLogTypeEnum;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserProfileService;
import com.rally.user.convert.ProfileAppConvertMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class ProfileAppService {

    @Resource
    private TennisProfileGateway tennisProfileGateway;

    @Resource
    private ProfileChangeLogGateway profileChangeLogGateway;

    @Resource
    private UserGateway userGateway;

    @Resource
    private ConfigGateway configGateway;

    @Resource
    private UserProfileService userProfileService;

    /**
     * 我的档案
     */
    public MyUserProfileDTO getMyProfile() {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileService.getProfile(userId);

        // 计算核查期剩余场次
        Integer reviewRemaining = null;
        if (userProfile.isUnderReview()) {
            reviewRemaining = userProfileService.getReviewRemainingMatches(userId);
        }

        // 计算 NTRP 可编辑状态和冷却剩余天数
        int lowDays = configGateway.getInt("score.ntrp.cooldown_low_days", 30);
        int midDays = configGateway.getInt("score.ntrp.cooldown_mid_days", 60);
        int highDays = configGateway.getInt("score.ntrp.cooldown_high_days", 90);
        int cooldown = userProfile.calculateNtrpCooldownDays(lowDays, midDays, highDays);

        Object[] editableStatus = userProfile.calculateNtrpEditableStatus(cooldown);
        Boolean ntrpEditable = (Boolean) editableStatus[0];
        Integer cooldownDays = (Integer) editableStatus[1];

        return ProfileAppConvertMapper.INSTANCE.toMyProfileVO(userProfile.getProfile(), userProfile.getUser(), reviewRemaining, ntrpEditable, cooldownDays);
    }

    /**
     * 球员主页
     */
    public PlayerHomeVO getPlayerHome(String targetUserId) {
        TennisProfileData profileData = tennisProfileGateway.findByUserId(targetUserId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        UserData userData = userGateway.findByUserId(targetUserId).orElse(null);

        return ProfileAppConvertMapper.INSTANCE.toPlayerHomeVO(profileData, userData, null);
    }

    /**
     * 编辑资料
     */
    @Transactional
    public MyUserProfileDTO editProfile(EditProfileCmd cmd) {
        String userId = UserContext.get();
        // 更新 users 表（头像/昵称/性别/生日）
        UserData userData = userGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.DATA_NOT_FOUND, "用户不存在"));
        if (cmd.getNickname() != null) {
            userData.setNickname(cmd.getNickname());
        }
        if (cmd.getAvatarUrl() != null) {
            userData.setAvatarUrl(cmd.getAvatarUrl());
        }
        if (cmd.getGender() != null) {
            try {
                userData.setGender(com.rally.domain.user.enums.GenderEnum.valueOf(cmd.getGender().toUpperCase()));
            } catch (IllegalArgumentException e) {
                throw new BusinessException(BizErrorCode.PARAM_ERROR, "性别值非法");
            }
        }
        if (cmd.getBirthday() != null) {
            userData.setBirthday(cmd.getBirthday());
        }
        if (cmd.getBio() != null) {
            userData.setBio(cmd.getBio());
        }
        userGateway.updateUser(userData);

        // 更新 profile 表（城市）
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        if (cmd.getCityCode() != null) {
            profileData.setCityCode(cmd.getCityCode());
        }
        tennisProfileGateway.update(profileData);

        return getMyProfile();
    }

    /**
     * 自评修改
     */
    @Transactional
    public MyUserProfileDTO updateNtrp(NtrpUpdateCmd cmd) {
        String userId = UserContext.get();
        // 1. 校验 NTRP 值
        if (cmd.getNtrpScore() == null) {
            throw new BusinessException(BizErrorCode.NTRP_INVALID_VALUE, "自评分值不能为空");
        }
        BigDecimal ntrp = cmd.getNtrpScore();
        // 校验 1.5~7.0 步长 0.5
        if (ntrp.compareTo(new BigDecimal("1.5")) < 0 || ntrp.compareTo(new BigDecimal("7.0")) > 0) {
            throw new BusinessException(BizErrorCode.NTRP_INVALID_VALUE, "自评分值必须在 1.5-7.0 之间");
        }
        if (ntrp.multiply(new BigDecimal("2")).remainder(new BigDecimal("1")).compareTo(BigDecimal.ZERO) != 0) {
            throw new BusinessException(BizErrorCode.NTRP_INVALID_VALUE, "自评分值步长必须为 0.5");
        }

        // 2. 获取档案
        UserProfile userProfile = userProfileService.getProfile(userId);
        TennisProfileData profileData = userProfile.getProfile();

        // 3. 冷却校验（system_suggest 跳过）
        if (profileData.getNtrpUpdatedAt() != null && !Boolean.TRUE.equals(cmd.getConfirmed())) {
            int lowDays = configGateway.getInt("score.ntrp.cooldown_low_days", 30);
            int midDays = configGateway.getInt("score.ntrp.cooldown_mid_days", 60);
            int highDays = configGateway.getInt("score.ntrp.cooldown_high_days", 90);
            int cooldown = userProfile.calculateNtrpCooldownDays(lowDays, midDays, highDays);

            long daysSinceUpdate = ChronoUnit.DAYS.between(profileData.getNtrpUpdatedAt(), LocalDateTime.now());
            if (daysSinceUpdate < cooldown) {
                throw new BusinessException(BizErrorCode.NTRP_COOLDOWN, "自评修改冷却中，" + (cooldown - daysSinceUpdate) + " 天后可改");
            }
        }

        // 4. 计算 delta
        BigDecimal oldNtrp = profileData.getNtrpScore();
        BigDecimal delta = oldNtrp != null ? ntrp.subtract(oldNtrp) : BigDecimal.ZERO;
        BigDecimal triggerDelta = new BigDecimal(configGateway.getString("score.review_period.trigger_ntrp_delta", "0.5"));

        // 5. 向上 >= 0.5 级触发核查期
        if (delta.compareTo(triggerDelta) >= 0) {
            int requiredMatches = configGateway.getInt("score.review_period.required_matches", 3);
            profileData.setStatus(ProfileStatusEnum.UNDER_REVIEW);
            profileData.setIsUnderReview(true);

            // 写核查期日志
            ProfileChangeLogData changeLog = new ProfileChangeLogData();
            changeLog.setUserId(userId);
            changeLog.setType(ChangeLogTypeEnum.UNDER_REVIEW);
            changeLog.setBeforeValue(new BigDecimal(requiredMatches));
            changeLog.setAfterValue(new BigDecimal(requiredMatches));
            changeLog.setReason(ChangeReasonEnum.USER);
            changeLog.setRemark("自评向上修改触发核查期");
            profileChangeLogGateway.save(changeLog);
        }

        // 6. 更新 NTRP
        profileData.setNtrpScore(ntrp);
        profileData.setNtrpUpdatedAt(LocalDateTime.now());
        tennisProfileGateway.update(profileData);

        // 7. 写 NTRP 变更日志
        ProfileChangeLogData ntrpLog = new ProfileChangeLogData();
        ntrpLog.setUserId(userId);
        ntrpLog.setType(ChangeLogTypeEnum.NTRP);
        ntrpLog.setBeforeValue(oldNtrp);
        ntrpLog.setAfterValue(ntrp);
        ntrpLog.setValue(delta);
        ntrpLog.setReason(ChangeReasonEnum.USER);
        profileChangeLogGateway.save(ntrpLog);

        return getMyProfile();
    }

    /**
     * 推进核查期进度（由评分域调用）
     */
    @Transactional
    public void advanceReviewProgress(String userId, String meetupId, boolean isBad) {
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));

        if (!profileData.getIsUnderReview()) {
            return;
        }

        Optional<ProfileChangeLogData> latestLog = profileChangeLogGateway.findLatestUnderReviewLog(userId);
        if (latestLog.isEmpty()) {
            return;
        }

        BigDecimal remaining = latestLog.get().getAfterValue();
        if (remaining == null || remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        if (isBad) {
            // 遇差票：重置进度，可信度暂降
            int requiredMatches = configGateway.getInt("score.review_period.required_matches", 3);
            int penaltyCredibility = configGateway.getInt("score.review_period.penalty_credibility", 50);

            ProfileChangeLogData resetLog = new ProfileChangeLogData();
            resetLog.setUserId(userId);
            resetLog.setType(ChangeLogTypeEnum.UNDER_REVIEW);
            resetLog.setBeforeValue(remaining);
            resetLog.setAfterValue(new BigDecimal(requiredMatches));
            resetLog.setReason(ChangeReasonEnum.REVIEW_BAD);
            resetLog.setRefId(meetupId);
            resetLog.setRemark("遇差票重置核查期进度");
            profileChangeLogGateway.save(resetLog);

            // 冻结可信度为 50
            tennisProfileGateway.updateScoreFields(userId, null,
                    new BigDecimal(penaltyCredibility), null, null);
        } else {
            // 正常推进
            BigDecimal newRemaining = remaining.subtract(BigDecimal.ONE);

            ProfileChangeLogData advanceLog = new ProfileChangeLogData();
            advanceLog.setUserId(userId);
            advanceLog.setType(ChangeLogTypeEnum.UNDER_REVIEW);
            advanceLog.setBeforeValue(remaining);
            advanceLog.setAfterValue(newRemaining);
            advanceLog.setReason(ChangeReasonEnum.SYSTEM);
            advanceLog.setRefId(meetupId);
            profileChangeLogGateway.save(advanceLog);

            // 检查是否解除
            if (newRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                releaseReview(userId);
            }
        }
    }

    /**
     * 解除核查期（由评分域调用）
     */
    @Transactional
    public void releaseReview(String userId) {
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));

        profileData.setStatus(ProfileStatusEnum.NORMAL);
        profileData.setIsUnderReview(false);
        tennisProfileGateway.update(profileData);
    }
}
