package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.recap.enums.SetFormatEnum;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.UserMeetupListCmd;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.meetup.MeetupCardPackingService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.service.RecapDomainService;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.user.model.*;
import com.rally.domain.user.service.UserFollowDomainService;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
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

    private static final int LIST_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

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
    private RecapDomainService recapDomainService;

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
                .setVideo(buildVideoDTO(profileData))
                .setSetScore(buildSetScoreDTO(targetUserId));
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
                .setAvatarUrl(buildSignedUrl(userData.getAvatarUrl()))
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
                            .setUrl(buildSignedUrl(video.getKey()))
                            .setCoverUrl(QiniuConfiguration.buildCover(video.getKey()))
                            .setTitle(StringUtils.isBlank(video.getTitle()) ? "未命名" : video.getTitle()))
                    .collect(Collectors.toList()));
        } else {
            videoDTO.setTotal(0);
            videoDTO.setData(new ArrayList<>());
        }
        return videoDTO;
    }

    /** 构建最近10场比分 DTO（头像使用比分记录中的冗余数据） */
    private MyProfileSetScoreDTO buildSetScoreDTO(String userId) {
        List<ScoreRecordData> records = recapDomainService.listScoresByUserId(userId);
        long singleCount = records.stream().filter(r -> r.getMatchType() == MatchTypeEnum.SINGLE).count();
        long doubleCount = records.stream().filter(r -> r.getMatchType() == MatchTypeEnum.DOUBLE).count();
        List<MyProfileSetScoreDTO.SetItem> setItems = records.stream()
                .limit(LIST_LIMIT)
                .map(r -> buildSetItem(userId, r))
                .toList();
        return new MyProfileSetScoreDTO()
                .setTotal((long) records.size())
                .setSingleCount(singleCount)
                .setDoubleCount(doubleCount)
                .setSetItems(setItems);
    }

    private MyProfileSetScoreDTO.SetItem buildSetItem(String userId, ScoreRecordData record) {
        boolean userInSideA = userId.equals(record.getSideAPlayer1()) || userId.equals(record.getSideAPlayer2());
        boolean isWin = (userInSideA && "A".equals(record.getWinSide())) || (!userInSideA && "B".equals(record.getWinSide()));
        ResultTypeEnum resultType = isWin ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;
        String matchTypeLabel = record.getMatchType() == MatchTypeEnum.DOUBLE ? "双打" : "单打";
        String formatLabel = record.getSetFormat() == SetFormatEnum.TIEBREAK ? "抢分" : "局";
        String title = record.getMeetupDate().format(DATE_FORMATTER) + " · " + matchTypeLabel + " · " + formatLabel;
        return new MyProfileSetScoreDTO.SetItem()
                .setTitle(title)
                .setResultType(resultType)
                .setMatchType(record.getMatchType())
                .setSideAPlayer1AvatarUrl(buildSignedUrl(record.getSideAPlayer1Avatar()))
                .setSideAPlayer2AvatarUrl(buildSignedUrl(record.getSideAPlayer2Avatar()))
                .setSideAScore(String.valueOf(record.getSideAScore()))
                .setSideBPlayer1AvatarUrl(buildSignedUrl(record.getSideBPlayer1Avatar()))
                .setSideBPlayer2AvatarUrl(buildSignedUrl(record.getSideBPlayer2Avatar()))
                .setSideBScore(String.valueOf(record.getSideBScore()));
    }

    private String buildSignedUrl(String key) {
        return StringUtils.isBlank(key) ? null : QiniuConfiguration.buildSignedUrl(key);
    }
}
