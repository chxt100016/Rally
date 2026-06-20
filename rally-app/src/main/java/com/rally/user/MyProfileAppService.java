package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.service.RecapDomainService;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
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

    @Resource
    private RecapDomainService recapDomainService;

    @Resource
    private UserFollowDomainService userFollowDomainService;

    /** 战绩明细展示数量 */
    private static final int SET_SCORE_ITEM_LIMIT = 5;

    /** 战绩明细标题日期格式 */
    private static final DateTimeFormatter SET_TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

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
                .setReview(hasProfile ? buildReviewDTO(userId) : null)
                .setVideo(hasProfile ? buildVideoDTO(userProfile.getProfile()) : null)
                .setSetScore(hasProfile ? buildSetScoreDTO(userId) : null);

    }


    private MyProfileStatsDTO buildStats(String userId) {
        long completedCount = meetupDomainService.countFinishedMeetups(userId);
        return new MyProfileStatsDTO()
                .setFollowerCount(userFollowDomainService.countFollowers(userId))
                .setFollowingCount(userFollowDomainService.countFollowing(userId))
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

    /** 构建战绩信息 DTO（按比赛日期倒序，仅展示最近若干场明细） */
    private MyProfileSetScoreDTO buildSetScoreDTO(String userId) {
        List<ScoreRecordData> records = recapDomainService.listScoresByUserId(userId);
        long singleCount = records.stream().filter(record -> record.getMatchType() == MatchTypeEnum.SINGLE).count();
        long doubleCount = records.stream().filter(record -> record.getMatchType() == MatchTypeEnum.DOUBLE).count();
        // 批量获取所有玩家头像（避免从 score 表冗余读取）
        Map<String, UserProfile> profiles = batchFetchPlayerProfiles(records);
        List<MyProfileSetScoreDTO.SetItem> setItems = records.stream()
                .limit(SET_SCORE_ITEM_LIMIT)
                .map(record -> buildSetItem(userId, record, profiles))
                .collect(Collectors.toList());
        return new MyProfileSetScoreDTO()
                .setTotal((long) records.size())
                .setSingleCount(singleCount)
                .setDoubleCount(doubleCount)
                .setSetItems(setItems);
    }

    /** 批量获取战绩中所有玩家的档案（用于取头像） */
    private Map<String, UserProfile> batchFetchPlayerProfiles(List<ScoreRecordData> records) {
        List<String> playerIds = records.stream()
                .flatMap(r -> Arrays.asList(
                        r.getSideAPlayer1(), r.getSideAPlayer2(),
                        r.getSideBPlayer1(), r.getSideBPlayer2()).stream())
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        return userProfileDomainService.listMap(playerIds);
    }

    /** 构建单条战绩明细：根据当前用户所在阵营与持久化的获胜边判断胜负 */
    private MyProfileSetScoreDTO.SetItem buildSetItem(String userId, ScoreRecordData record, Map<String, UserProfile> profiles) {
        boolean userInSideA = userId.equals(record.getSideAPlayer1()) || userId.equals(record.getSideAPlayer2());
        boolean isWin = (userInSideA && "A".equals(record.getWinSide())) || (!userInSideA && "B".equals(record.getWinSide()));
        ResultTypeEnum resultType = isWin ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;
        return new MyProfileSetScoreDTO.SetItem()
                .setResultType(resultType)
                .setResultTypeShow(resultType.getShow())
                .setMatchType(record.getMatchType())
                .setMatchTypeShow(record.getMatchType().getName())
                .setSetFormat(record.getSetFormat())
                .setSetFormatShow(record.getSetFormat().getShow())
                .setDate(record.getMeetupDate().format(SET_TITLE_DATE_FORMATTER))
                .setSideAPlayer1AvatarUrl(getAvatarUrl(profiles, record.getSideAPlayer1()))
                .setSideAPlayer2AvatarUrl(getAvatarUrl(profiles, record.getSideAPlayer2()))
                .setSideAScore(String.valueOf(record.getSideAScore()))
                .setSideBPlayer1AvatarUrl(getAvatarUrl(profiles, record.getSideBPlayer1()))
                .setSideBPlayer2AvatarUrl(getAvatarUrl(profiles, record.getSideBPlayer2()))
                .setSideBScore(String.valueOf(record.getSideBScore()));
    }

    /** 从 profiles map 获取玩家头像（带签名URL） */
    private String getAvatarUrl(Map<String, UserProfile> profiles, String playerId) {
        if (playerId == null) return null;
        UserProfile profile = profiles.get(playerId);
        if (profile == null || profile.getUser() == null) return null;
        return QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl());
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
