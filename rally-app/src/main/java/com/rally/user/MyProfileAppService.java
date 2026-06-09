package com.rally.user;

import com.rally.utils.UserContext;
import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.enums.ProfileStatusEnum;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserProfileDomainService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 我的档案应用服务
 * 负责 getMyProfile 及其子 DTO 构建
 */
@Slf4j
@Service
public class MyProfileAppService {

    @Resource
    private UserProfileDomainService userProfileDomainService;

    @Resource
    private MeetupDomainService meetupDomainService;

    @Resource
    private UserReviewDomainService userReviewDomainService;

    /**
     * 我的档案（新版）
     */
    public MyProfileDTO getMyProfile() {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);

        boolean isTBC = userProfile.getStatus() == ProfileStatusEnum.TBC;
        return new MyProfileDTO()
                .setStatus(userProfile.getStatus())
                .setUser(buildUserDTO(userProfile.getUser()))
                .setMeetup(isTBC ? null : buildMeetupDTO(userId))
                .setReview(isTBC ? null : buildReviewDTO(userId))
                .setLevel(isTBC ? null : buildLevelDTO(userProfile))
                .setScore(isTBC ? null : buildScoreDTO(userProfile.getProfile()))
                .setVideo(isTBC ? null : buildVideoDTO(userProfile.getProfile()));
    }

    // ========== 各子 DTO 构建方法 ==========

    /** 构建约球信息 DTO（通过领域服务查询已完成约球次数） */
    private MyProfileMeetupDTO buildMeetupDTO(String userId) {
        long completedCount = meetupDomainService.countFinishedMeetups(userId);
        return new MyProfileMeetupDTO().setCompletedCount((int) completedCount);
    }

    /** 构建评价信息 DTO（一次查库聚合评价总数+标签） */
    private MyProfileReviewDTO buildReviewDTO(String userId) {
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(userId, 2);
        return new MyProfileReviewDTO()
                .setTotal(summary.total())
                .setTags(summary.topTags().stream()
                        .map(tag -> new ReviewTagDTO().setName(tag))
                        .collect(Collectors.toList()));
    }

    /** 构建等级信息 DTO */
    private MyProfileLevelDTO buildLevelDTO(UserProfile userProfile) {
        TennisProfileData profileData = userProfile.getProfile();
        Integer cooldownDays = userProfile.calculateNtrpCooldownDays();
        Integer remainingMatches = userProfile.isUnderReview()
                ? profileData.getReviewRemainingMatches() : null;
        return new MyProfileLevelDTO()
                .setNtrpScore(profileData != null ? profileData.getNtrpScore() : null)
                .setIsUnderReview(profileData != null ? profileData.getIsUnderReview() : null)
                .setCooldownDays(cooldownDays)
                .setRemainingMatches(remainingMatches);
    }

    /** 构建评分信息 DTO（评分明细权重从 SystemConfig 读取） */
    private MyProfileScoreDTO buildScoreDTO(TennisProfileData profileData) {
        String scoreLevel = ProfileLevelManager.calculate(profileData);
        return new MyProfileScoreDTO()
                .setProfileLevel(scoreLevel)
                .setData(buildScoreItemList(profileData));
    }

    /** 构建用户基本信息 DTO */
    private MyProfileUserDTO buildUserDTO(UserData userData) {
        if (userData == null) {
            return new MyProfileUserDTO();
        }
        return new MyProfileUserDTO()
                .setUserId(userData.getUserId())
                .setNickname(userData.getNickname())
                .setAvatarUrl(userData.getAvatarUrl())
                .setGender(userData.getGender())
                .setBirthday(userData.getBirthday())
                .setCityCode(userData.getCityCode())
                .setBio(userData.getBio());
    }

    /** 构建视频信息 DTO */
    private MyProfileVideoDTO buildVideoDTO(TennisProfileData profileData) {
        MyProfileVideoDTO videoDTO = new MyProfileVideoDTO();
        if (profileData != null && profileData.getVideoUrls() != null) {
            List<String> videoKeys = profileData.getVideoUrls();
            videoDTO.setTotal(videoKeys.size());
            videoDTO.setData(videoKeys.stream()
                    .map(key -> new VideoItemDTO().setKey(key).setUrl(QiniuConfiguration.buildSignedUrl(key)))
                    .collect(Collectors.toList()));
        } else {
            videoDTO.setTotal(0);
            videoDTO.setData(new ArrayList<>());
        }
        return videoDTO;
    }

    // ========== 评分明细内部方法 ==========

    /** 构建评分明细列表 */
    private List<ScoreItemDTO> buildScoreItemList(TennisProfileData profileData) {
        List<ScoreItemDTO> items = new ArrayList<>();
        items.add(buildOneScoreItem("信誉分", "reputation_score",
                profileData != null ? profileData.getReputationScore() : null,
                "score.weight.reputation", 50, "score.info.reputation", "信誉分说明你有信誉"));
        items.add(buildOneScoreItem("可信度", "credibility_score",
                profileData != null ? profileData.getCredibilityScore() : null,
                "score.weight.credibility", 30, "score.info.credibility", "可信度说明你可信"));
        items.add(buildOneScoreItem("校准度", "calibration_score",
                profileData != null ? profileData.getCalibrationScore() : null,
                "score.weight.calibration", 20, "score.info.calibration", "校准度说明你校准"));
        return items;
    }

    /** 构建单个评分明细项 */
    private ScoreItemDTO buildOneScoreItem(String name, String key, BigDecimal score, String weightConfigKey, int defaultWeight, String infoConfigKey, String defaultInfo) {
        return new ScoreItemDTO()
                .setName(name)
                .setKey(key)
                .setValue(score != null ? score.toPlainString() : "0")
                .setLabel("权重" + SystemConfig.getInt(weightConfigKey, defaultWeight) + "%")
                .setInfo(SystemConfig.getString(infoConfigKey, defaultInfo))
                .setSort("");
    }
}
