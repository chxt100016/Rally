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
import com.rally.domain.user.gateway.VideoUploadGateway;
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
    private VideoUploadGateway videoUploadGateway;

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

        // 签名视频 URL
        List<String> signedUrls = signVideoUrls(profileData.getVideoUrls());
        profileData.setVideoUrls(signedUrls);

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
        userGateway.updateUser(userData);

        // 更新 profile 表（城市/简介）
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        if (cmd.getCityCode() != null) {
            profileData.setCityCode(cmd.getCityCode());
        }
        if (cmd.getBio() != null) {
            profileData.setBio(cmd.getBio());
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
     * 取视频直传凭证
     */
    public VideoTokenVO getVideoUploadToken() {
        String userId = UserContext.get();
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElse(null);
        if (profileData != null) {
            int maxCount = configGateway.getInt("user.video.max_count", 3);
            List<String> currentUrls = profileData.getVideoUrls();
            if (currentUrls != null && currentUrls.size() >= maxCount) {
                throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED);
            }
        }

        int maxSizeMb = configGateway.getInt("user.video.max_size_mb", 5);
        return videoUploadGateway.generateUploadToken(userId, maxSizeMb);
    }

    /**
     * 七牛回调入库
     */
    @Transactional
    public void handleVideoCallback(VideoCallbackCmd cmd) {
        String userId = cmd.getUserId();
        String key = cmd.getKey();

        // 校验 key 前缀
        if (!key.startsWith("videos/" + userId + "/")) {
            throw new BusinessException(BizErrorCode.VIDEO_NOT_OWNED);
        }

        // 校验文件大小
        int maxSizeMb = configGateway.getInt("user.video.max_size_mb", 5);
        if (cmd.getFsize() != null && cmd.getFsize() > maxSizeMb * 1024 * 1024) {
            throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED, "视频文件过大");
        }

        // 校验时长
        int maxDuration = configGateway.getInt("user.video.max_duration_sec", 60);
        if (cmd.getDuration() != null && cmd.getDuration() > maxDuration) {
            throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED, "视频时长超限");
        }

        // 追加到 video_urls
        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        List<String> videoUrls = profileData.getVideoUrls();
        if (videoUrls == null) {
            videoUrls = new ArrayList<>();
        }

        int maxCount = configGateway.getInt("user.video.max_count", 3);
        if (videoUrls.size() >= maxCount) {
            throw new BusinessException(BizErrorCode.VIDEO_LIMIT_EXCEEDED);
        }

        videoUrls.add(key);
        tennisProfileGateway.updateVideoUrls(userId, videoUrls);
    }

    /**
     * 删除视频
     */
    @Transactional
    public void deleteVideo(String key) {
        String userId = UserContext.get();
        // 校验 key 前缀
        if (!key.startsWith("videos/" + userId + "/")) {
            throw new BusinessException(BizErrorCode.VIDEO_NOT_OWNED);
        }

        TennisProfileData profileData = tennisProfileGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));
        List<String> videoUrls = profileData.getVideoUrls();
        if (videoUrls == null) {
            videoUrls = new ArrayList<>();
        }

        videoUrls.remove(key);
        tennisProfileGateway.updateVideoUrls(userId, videoUrls);
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

    /**
     * 签名视频 URL
     */
    private List<String> signVideoUrls(List<String> keys) {
        if (keys == null || keys.isEmpty()) {
            return List.of();
        }
        int maxCount = configGateway.getInt("user.video.max_count", 3);
        return keys.stream()
                .limit(maxCount)
                .map(key -> {
                    // 这里需要 QiniuClient 来签名，但 app 层不直接依赖 infrastructure
                    // 暂时返回原始 key，实际应通过 gateway 或 service 调用
                    return key;
                })
                .toList();
    }
}
