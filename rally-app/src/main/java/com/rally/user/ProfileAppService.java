package com.rally.user;

import com.rally.client.qiniu.QiniuClient;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.log.model.ProfileChangeLogData;
import com.rally.utils.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.log.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.*;
import com.rally.domain.log.ProfileLogService;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.domain.utils.Assert;
import com.rally.db.user.convert.UserConvertMapper;
import com.rally.user.convert.UserAppConvertMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private ProfileLogService profileRecordService;

    @Resource
    private UserGateway userGateway;

    @Resource
    private UserProfileDomainService userProfileDomainService;

    @Resource
    private MyProfileAppService myProfileAppService;

    @Resource
    private QiniuClient qiniuClient;

    /**
     * 编辑资料
     */
    @Transactional
    public MyProfileDTO editUser(EditProfileCmd cmd) {
        String userId = UserContext.get();
        UserData userData = userGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.DATA_NOT_FOUND, "用户不存在"));
        UserConvertMapper.INSTANCE.updateData(userData, cmd);
        userGateway.updateUser(userData);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 上传视频，追加到我的视频列表
     */
    @Transactional
    public MyProfileDTO uploadVideo(UploadVideoCmd cmd) {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);
        QiniuConfiguration.buildSignedUrl(cmd.getKey());
        userProfile.addVideo(UserAppConvertMapper.INSTANCE.toVideoVO(cmd));
        userProfileDomainService.save(userProfile);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 删除视频
     */
    @Transactional
    public MyProfileDTO deleteVideo(DeleteVideoCmd cmd) {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);

        // 校验至少保留一个视频
        List<VideoVO> videos = userProfile.getProfile().getVideos();
        Assert.isTrue(videos != null && videos.size() > 1, BizErrorCode.VIDEO_AT_LEAST_ONE);

        userProfile.deleteVideo(cmd.getKey());
        userProfileDomainService.save(userProfile);

        // 删除七牛云视频
        qiniuClient.deleteFile(cmd.getKey());

        return myProfileAppService.getMyProfile();
    }

    /**
     * 修改视频
     */
    @Transactional
    public MyProfileDTO updateVideo(UpdateVideoCmd cmd) {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);
        userProfile.updateVideo(cmd.getKey(), cmd.getTitle());
        userProfileDomainService.save(userProfile);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 自评修改
     */
    @Transactional
    public MyProfileDTO updateNtrp(NtrpUpdateCmd cmd) {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);
        userProfileDomainService.updateNtrp(userProfile, cmd.getNtrpScore());

        return myProfileAppService.getMyProfile();
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
            int requiredMatches = SystemConfig.getInt(SystemConfigKey.SCORE_REVIEW_PERIOD_REQUIRED_MATCHES.getKey());
            int penaltyCredibility = SystemConfig.getInt(SystemConfigKey.SCORE_REVIEW_PERIOD_PENALTY_CREDIBILITY.getKey());
            profileRecordService.saveReviewResetLog(userId, remaining, requiredMatches, meetupId);
            tennisProfileGateway.updateScoreFields(userId, null,
                    penaltyCredibility, null, null);
        } else {
            BigDecimal newRemaining = remaining.subtract(BigDecimal.ONE);
            profileRecordService.saveReviewAdvanceLog(userId, remaining, newRemaining, meetupId);
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
