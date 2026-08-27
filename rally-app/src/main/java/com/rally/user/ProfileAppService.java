package com.rally.user;

import com.rally.domain.log.model.ProfileChangeLogData;
import com.rally.personalprofile.basicprofileupdate.activity.UpdateBasicProfileActivity;
import com.rally.personalprofile.genderupdate.activity.UpdateGenderActivity;
import com.rally.personalprofile.profilevideoadd.activity.AppendProfileVideoActivity;
import com.rally.personalprofile.profilevideodelete.activity.DeleteProfileVideoFileActivity;
import com.rally.personalprofile.profilevideodelete.activity.RemoveProfileVideoItemsActivity;
import com.rally.personalprofile.profilevideoupdate.activity.UpdateProfileVideoTitleActivity;
import com.rally.personalprofile.selfratingupdate.activity.RecordReviewTriggerActivity;
import com.rally.personalprofile.selfratingupdate.activity.RecordSelfRatingChangeActivity;
import com.rally.personalprofile.selfratingupdate.activity.SelfRatingUpdateContext;
import com.rally.personalprofile.selfratingupdate.activity.UpdateSelfRatingProfileActivity;
import com.rally.utils.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.log.gateway.ProfileChangeLogRepository;
import com.rally.domain.user.gateway.TennisProfileRepository;
import com.rally.domain.user.gateway.UserRepository;
import com.rally.domain.user.model.*;
import com.rally.domain.log.ProfileLogService;
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
    private TennisProfileRepository tourProfileRepository;

    @Resource
    private ProfileChangeLogRepository profileChangeLogRepository;

    @Resource
    private ProfileLogService profileRecordService;

    @Resource
    private UserRepository userRepository;

    @Resource
    private MyProfileAppService myProfileAppService;

    @Resource
    private UpdateBasicProfileActivity updateBasicProfileActivity;

    @Resource
    private UpdateGenderActivity updateGenderActivity;

    @Resource
    private AppendProfileVideoActivity appendProfileVideoActivity;

    @Resource
    private RemoveProfileVideoItemsActivity removeProfileVideoItemsActivity;

    @Resource
    private DeleteProfileVideoFileActivity deleteProfileVideoFileActivity;

    @Resource
    private UpdateProfileVideoTitleActivity updateProfileVideoTitleActivity;

    @Resource
    private UpdateSelfRatingProfileActivity updateSelfRatingProfileActivity;

    @Resource
    private RecordSelfRatingChangeActivity recordSelfRatingChangeActivity;

    @Resource
    private RecordReviewTriggerActivity recordReviewTriggerActivity;


    /**
     * 编辑资料
     */
    @Transactional
    public MyProfileDTO editUser(EditProfileCmd cmd) {
        String userId = UserContext.get();
        updateBasicProfileActivity.execute(userId, cmd);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 上传视频，追加到我的视频列表
     */
    @Transactional
    public MyProfileDTO uploadVideo(UploadVideoCmd cmd) {
        String userId = UserContext.get();
        appendProfileVideoActivity.execute(userId, cmd);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 删除视频
     */
    @Transactional
    public MyProfileDTO deleteVideo(DeleteVideoCmd cmd) {
        String userId = UserContext.get();
        removeProfileVideoItemsActivity.execute(userId, cmd.getKey());

        deleteProfileVideoFileActivity.execute(cmd.getKey());

        return myProfileAppService.getMyProfile();
    }

    /**
     * 修改视频
     */
    @Transactional
    public MyProfileDTO updateVideo(UpdateVideoCmd cmd) {
        String userId = UserContext.get();
        updateProfileVideoTitleActivity.execute(userId, cmd.getKey(), cmd.getTitle());

        return myProfileAppService.getMyProfile();
    }

    /**
     * 修改性别
     */
    @Transactional
    public MyProfileDTO updateGender(UpdateGenderCmd cmd) {
        String userId = UserContext.get();
        updateGenderActivity.execute(userId, cmd);

        return myProfileAppService.getMyProfile();
    }

    /**
     * 自评修改
     */
    @Transactional
    public MyProfileDTO updateNtrp(NtrpUpdateCmd cmd) {
        String userId = UserContext.get();
        SelfRatingUpdateContext context = updateSelfRatingProfileActivity.execute(userId, cmd.getNtrpScore());
        recordSelfRatingChangeActivity.execute(context.userId(), context.oldNtrp(), context.newNtrp());
        recordReviewTriggerActivity.execute(context.userId(), context.requiredMatches());

        return myProfileAppService.getMyProfile();
    }

    /**
     * 推进核查期进度（由评分域调用）
     */
    @Transactional
    public void advanceReviewProgress(String userId, String meetupId, boolean isBad) {
        TennisProfileData profileData = tourProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));

        if (!profileData.getIsUnderReview()) {
            return;
        }

        Optional<ProfileChangeLogData> latestLog = profileChangeLogRepository.findLatestUnderReviewLog(userId);
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
            tourProfileRepository.updateScoreFields(userId, null,
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
        TennisProfileData profileData = tourProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(BizErrorCode.PROFILE_NOT_FOUND));

        profileData.setStatus(ProfileStatusEnum.NORMAL);
        profileData.setIsUnderReview(false);
        tourProfileRepository.update(profileData);
    }
}
