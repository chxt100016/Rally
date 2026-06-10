package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserProfileDomainService;
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
    private UserReviewDomainService userReviewDomainService;

    /**
     * 球员主页
     */
    public PlayerHomeDTO getPlayerHome(String targetUserId) {
        UserProfile userProfile = userProfileDomainService.get(targetUserId);
        UserData userData = userProfile.getUser();
        TennisProfileData profileData = userProfile.getProfile();

        return new PlayerHomeDTO()
                .setUser(buildUserDTO(userData))
                .setMeetup(buildMeetupDTO(targetUserId))
                .setReview(buildReviewDTO(targetUserId))
                .setLevel(buildLevelDTO(profileData))
                .setScore(buildScoreDTO(profileData))
                .setVideo(buildVideoDTO(profileData));
    }

    // ========== 各子 DTO 构建方法 ==========

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

    /** 构建约球信息 DTO */
    private PlayerHomeMeetupDTO buildMeetupDTO(String userId) {
        long completedCount = meetupDomainService.countFinishedMeetups(userId);
        UserMeetupListCmd recentCmd = new UserMeetupListCmd();
        recentCmd.setTab(UserMeetupTabEnum.RECENT);
        recentCmd.setPageSize(3);
        List<MeetupCardDTO> recentMeetups = meetupQueryDomainService.listByUser(recentCmd, userId).getList();
        return new PlayerHomeMeetupDTO().setCompletedCount((int) completedCount).setRecentMeetups(recentMeetups);
    }

    /** 构建评价信息 DTO */
    private MyProfileReviewDTO buildReviewDTO(String userId) {
        ReviewSummaryDTO summary = userReviewDomainService.getReviewSummary(userId, 5);
        return new MyProfileReviewDTO()
                .setTotal(summary.total())
                .setTags(summary.topTags().stream()
                        .map(tag -> new ReviewTagDTO().setName(tag.name()).setCount((int) tag.count()))
                        .collect(Collectors.toList()));
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
}
