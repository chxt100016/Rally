package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.system.CityConfig;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserFollowDomainService;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

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
    private UserMeetupQueryDomainService userMeetupQueryDomainService;

    @Resource
    private UserFollowDomainService userFollowDomainService;

    /**
     * 我的档案
     */
    public MyProfileDTO getMyProfile() {
        String userId = UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(userId);


        boolean hasProfile = userProfile.hasProfile();
        return new MyProfileDTO()
                .setStatus(userProfile.getStatus())
                .setUser(buildUserDTO(userProfile.getUser()))
                .setStats(hasProfile ? buildStats(userId) : null)
                .setLevel(hasProfile ? buildLevelDTO(userProfile) : null)
                .setScore(hasProfile ? buildScoreDTO(userProfile.getProfile()) : null)
                .setVideo(hasProfile ? buildVideoDTO(userProfile.getProfile()) : null);

    }


    private MyProfileStatsDTO buildStats(String userId) {
        long completedCount = userMeetupQueryDomainService.countCompleted(userId);
        return new MyProfileStatsDTO()
                .setFollowerCount(userFollowDomainService.countFollowers(userId))
                .setFollowingCount(userFollowDomainService.countFollowing(userId))
                .setCompletedCount(completedCount);
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
                .setSubTitle("NTRP 当前水平")
                .setNoticeTitle(noticeTitle)
                .setNoticeContent(noticeContent)
                .setNoticeInfo("手动修改后将进入 90天冻结期，请谨慎操作")
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
        items.add(buildOneScoreItem("信誉分", "reputation", profileData.getReputationScore(), SystemConfigKey.SCORE_INFO_REPUTATION, 1));
        items.add(buildOneScoreItem("可信度", "credibility", profileData.getCredibilityScore(), SystemConfigKey.SCORE_INFO_CREDIBILITY, 2));
        items.add(buildOneScoreItem("校准度", "calibration", profileData.getCalibrationScore(), SystemConfigKey.SCORE_INFO_CALIBRATION, 3));
        return items;
    }

    /** 构建单个评分明细项 */
    private ScoreItemDTO buildOneScoreItem(String name, String key, Integer score, SystemConfigKey infoConfigKey, Integer sort) {
        return new ScoreItemDTO()
                .setName(name)
                .setKey(key)
                .setValue(score != null ? String.valueOf(score) : "0")
                .setMaxValue(SystemConfig.getString(SystemConfigKey.SCORE_MAX.getKey()))
                .setInfo(SystemConfig.getString(infoConfigKey.getKey()))
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
                .setAvatarUrl(QiniuConfiguration.buildSignedUrl(userData.getAvatarUrl()))
                .setGender(userData.getGender())
                .setBirthday(userData.getBirthday())
                .setCityCode(userData.getCityCode())
                .setCityName(CityConfig.getCityName(userData.getCityCode()))
                .setBio(userData.getBio());
    }

    /** 构建视频信息 DTO */
    private MyProfileVideoDTO buildVideoDTO(TennisProfileData profileData) {
        MyProfileVideoDTO videoDTO = new MyProfileVideoDTO();
        List<VideoVO> videos = profileData.getVideos();
        videoDTO.setTotal(videos.size());
        videoDTO.setData(videos.stream()
                .map(video -> new VideoItemDTO()
                        .setKey(video.getKey())
                        .setUrl(QiniuConfiguration.buildSignedUrl(video.getKey()))
                        .setCoverUrl(QiniuConfiguration.buildCover(video.getKey()))
                        .setTitle(StringUtils.isBlank(video.getTitle()) ? "未命名" : video.getTitle()))
                .collect(Collectors.toList()));
        videoDTO.setMaxCount(SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_COUNT.getKey()));
        videoDTO.setMaxSizeMb(SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_SIZE_MB.getKey()));
        videoDTO.setMaxSecond(SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_SECOND.getKey()));
        return videoDTO;
    }
}
