package com.rally.user;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.gateway.ProfileChangeLogGateway;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.ProfileRecordService;
import com.rally.domain.user.service.UserProfileService;
import com.rally.db.user.convert.UserConvertMapper;
import com.rally.user.convert.ProfileAppConvertMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Slf4j
@Service
public class ProfileAppService {

    @Resource
    private TennisProfileGateway tennisProfileGateway;

    @Resource
    private ProfileChangeLogGateway profileChangeLogGateway;

    @Resource
    private ProfileRecordService profileRecordService;

    @Resource
    private UserGateway userGateway;

    @Resource
    private UserProfileService userProfileService;

    @Resource
    private MyProfileAppService myProfileAppService;

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
    public MyProfileDTO editUser(EditProfileCmd cmd) {
        String userId = UserContext.get();
        UserData userData = userGateway.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.DATA_NOT_FOUND, "用户不存在"));
        UserConvertMapper.INSTANCE.updateData(userData, cmd);
        userGateway.updateUser(userData);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 自评修改
     */
    @Transactional
    public MyProfileDTO updateNtrp(NtrpUpdateCmd cmd) {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileService.getProfile(userId);
        userProfileService.updateNtrp(userProfile, cmd.getNtrpScore());

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
            int requiredMatches = SystemConfig.getInt("score.review_period.required_matches", 3);
            int penaltyCredibility = SystemConfig.getInt("score.review_period.penalty_credibility", 50);
            profileRecordService.saveReviewResetLog(userId, remaining, requiredMatches, meetupId);
            tennisProfileGateway.updateScoreFields(userId, null,
                    new BigDecimal(penaltyCredibility), null, null);
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
