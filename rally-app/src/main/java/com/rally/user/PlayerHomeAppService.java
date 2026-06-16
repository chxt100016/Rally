package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.meetup.MeetupCardPackingService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserFollowDomainService;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 球员主页应用服务
 * 负责 getPlayerHome 及其子 DTO 构建
 */
@Slf4j
@Service
public class PlayerHomeAppService {

    @Resource
    private UserProfileDomainService userProfileDomainService;

    @Resource
    private MeetupDomainService meetupDomainService;

    @Resource
    private MeetupQueryDomainService meetupQueryDomainService;

    @Resource
    private MeetupCardPackingService meetupCardPackingService;

    @Resource
    private UserReviewDomainService userReviewDomainService;

    @Resource
    private UserFollowDomainService userFollowDomainService;

    /**
     * 球员主页
     */
    public PlayerHomeDTO getPlayerHome(String targetUserId) {
        // 校验登录
        UserContext.get();
        UserProfile userProfile = userProfileDomainService.get(targetUserId);
        UserData userData = userProfile.getUser();
        TennisProfileData profileData = userProfile.getProfile();

        return new PlayerHomeDTO()
                .setUser(buildUserDTO(userData))
                .setStats(buildStatsDTO(targetUserId))
                .setMeetup(buildMeetupDTO(targetUserId))
                .setReview(buildReviewDTO(targetUserId))
                .setLevel(buildLevelDTO(profileData))
                .setScore(buildScoreDTO(profileData))
                .setVideo(buildVideoDTO(profileData));
    }

    // ========== 各子 DTO 构建方法 ==========

    /** 构建关注统计 DTO（含当前登录用户是否已关注 TA） */
    private PlayerHomeStatsDTO buildStatsDTO(String targetUserId) {
        return new PlayerHomeStatsDTO()
                .setFollowerCount(userFollowDomainService.countFollowers(targetUserId))
                .setFollowingCount(userFollowDomainService.countFollowing(targetUserId))
                .setIsFollowed(userFollowDomainService.isFollowed(UserContext.get(), targetUserId));
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
                .setBio(userData.getBio());
    }

    /** 构建约球信息 DTO */
    private PlayerHomeMeetupDTO buildMeetupDTO(String userId) {
        long completedCount = meetupDomainService.countFinishedMeetups(userId);
        UserMeetupListCmd recentCmd = new UserMeetupListCmd();
        recentCmd.setTab(UserMeetupTabEnum.RECENT);
        recentCmd.setSize(3);
        List<MeetupData> recentDataList = meetupQueryDomainService.listByUser(recentCmd, userId).getList();
        List<MeetupCardDTO> recentMeetups = recentDataList.stream()
                .map(data -> meetupCardPackingService.packCardForTab(data, UserMeetupTabEnum.RECENT))
                .toList();
        return new PlayerHomeMeetupDTO().setCompletedCount((int) completedCount).setRecentMeetups(recentMeetups);
    }

    /** 构建评价信息 DTO */
    private MyProfileReviewDTO buildReviewDTO(String userId) {
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(userId, 5);
        return new MyProfileReviewDTO()
                .setTotal(summary.total())
                .setTags(summary.topTags());
    }

    /** 构建等级信息 DTO */
    private PlayerHomeLevelDTO buildLevelDTO(TennisProfileData profileData) {
        if (profileData == null) {
            return new PlayerHomeLevelDTO();
        }
        return new PlayerHomeLevelDTO()
                .setNtrpScore(profileData.getNtrpScore())
                .setIsUnderReview(profileData.getIsUnderReview())
                .setIsNewbie(profileData.getIsNewbie());
    }

    /** 构建评分信息 DTO */
    private PlayerHomeScoreDTO buildScoreDTO(TennisProfileData profileData) {
        String scoreLevel = ProfileLevelManager.calculate(profileData);
        return new PlayerHomeScoreDTO().setProfileLevel(scoreLevel);
    }

    /** 构建视频信息 DTO */
    private MyProfileVideoDTO buildVideoDTO(TennisProfileData profileData) {
        MyProfileVideoDTO videoDTO = new MyProfileVideoDTO();
        if (profileData != null && profileData.getVideos() != null) {
            List<VideoVO> videos = profileData.getVideos();
            videoDTO.setTotal(videos.size());
            videoDTO.setData(videos.stream()
                    .map(video -> new VideoItemDTO()
                            .setKey(video.getKey())
                            .setUrl(QiniuConfiguration.buildSignedUrl(video.getKey()))
                            .setTitle(video.getTitle()))
                    .collect(Collectors.toList()));
        } else {
            videoDTO.setTotal(0);
            videoDTO.setData(new ArrayList<>());
        }
        return videoDTO;
    }
}
