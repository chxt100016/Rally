package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        userProfile.assertBasic();

        boolean hasProfile = userProfile.hasProfile();
        return new MyProfileDTO()
                .setStatus(userProfile.getStatus())
                .setUser(buildUserDTO(userProfile.getUser()))
                .setStats(hasProfile ? buildStats(userId) : null)
                .setLevel(hasProfile ? buildLevelDTO(userProfile) : null)
                .setScore(hasProfile ? buildScoreDTO(userProfile.getProfile()) : null)
                .setReview(hasProfile ? buildReviewDTO(userId) : null)
                .setVideo(hasProfile ? buildVideoDTO(userProfile.getProfile()) : null)
                .setSetScore(hasProfile ? MyProfileSetScoreDTO.mock() : null);

    }

    private MyProfileStatsDTO buildStats(String userId) {
        long completedCount = meetupDomainService.countFinishedMeetups(userId);
        return new MyProfileStatsDTO()
                .setFollowerCount(0L)
                .setFollowingCount(0L)
                .setCompletedCount(completedCount);
    }



    /** 构建评价信息 DTO（一次查库聚合评价总数+标签） */
    private MyProfileReviewDTO buildReviewDTO(String userId) {
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(userId, 5);
        return new MyProfileReviewDTO()
                .setTotal(summary.total())
                .setLevelVoteCount(summary.levelVoteCount())
                .setAttendanceVoteCount(summary.attendanceVoteCount())
                .setTagCount(summary.tagCount())
                .setTags(summary.topTags());
    }

    /** 构建等级信息 DTO */
    private MyProfileLevelDTO buildLevelDTO(UserProfile userProfile) {
        TennisProfileData profileData = userProfile.getProfile();
        Integer cooldownDays = userProfile.calculateNtrpCooldownDays();
        Integer remainingMatches = userProfile.isUnderReview() ? profileData.getReviewRemainingMatches() : null;


        String noticeTitle = "系统建议";
        String noticeContent = "根据近 20 场对战数据，系统评估你的真实水平约为 4.5，可考虑上调";
        if (cooldownDays != null) {
            noticeTitle = "冻结期";
            noticeContent= "冻结期不可修改ntrp等级";
        } else if (remainingMatches != null) {
            noticeTitle = "核查期";
            noticeContent= String.format("核查期还剩下%s场",  remainingMatches);
        }

        return new MyProfileLevelDTO()
                .setNtrpScore(profileData.getNtrpScore().setScale(1, RoundingMode.HALF_UP).toString())
                .setSubTitle("NTRP 当前水平 · 系统认证")
                .setNoticeTitle(noticeTitle)
                .setNoticeContent(noticeContent)
                .setNoticeInfo("手动修改后将进入 90 天 冻结期，期间系统不再自动调整你的水平，请谨慎操作")
                .setCanModify((cooldownDays == null || cooldownDays == 0) && (remainingMatches == null || remainingMatches == 0));

    }

    /** 构建评分信息 DTO（评分明细权重从 SystemConfig 读取） */
    private MyProfileScoreDTO buildScoreDTO(TennisProfileData profileData) {
        String scoreLevel = ProfileLevelManager.calculate(profileData);
        return new MyProfileScoreDTO()
                .setProfileLevel(scoreLevel)
                .setData(buildScoreItemList(profileData));
    }

    /** 构建评分明细列表 */
    private List<ScoreItemDTO> buildScoreItemList(TennisProfileData profileData) {
        List<ScoreItemDTO> items = new ArrayList<>();
        items.add(buildOneScoreItem("信誉分", "reputation", profileData.getReputationScore(), "score.info.reputation", 1));
        items.add(buildOneScoreItem("可信度", "credibility", profileData.getCredibilityScore(), "score.info.credibility", 2));
        items.add(buildOneScoreItem("校准度", "calibration", profileData.getCalibrationScore(), "score.info.calibration", 3));
        return items;
    }

    /** 构建单个评分明细项 */
    private ScoreItemDTO buildOneScoreItem(String name, String key, BigDecimal score, String infoConfigKey, Integer sort) {
        return new ScoreItemDTO()
                .setName(name)
                .setKey(key)
                .setValue(score != null ? score.toPlainString() : "0")
                .setMaxValue("1500")
                .setInfo(SystemConfig.getString(infoConfigKey, ""))
                .setSort(sort);
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
        List<String> videoKeys = profileData.getVideoUrls();
        videoDTO.setTotal(videoKeys.size());
        videoDTO.setData(videoKeys.stream()
                .map(key -> new VideoItemDTO().setKey(key).setUrl(QiniuConfiguration.buildSignedUrl(key)).setTitle("标题"))
                .collect(Collectors.toList()));
        return videoDTO;
    }
}
