package com.rally.user;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.UserReviewDomainService;
import com.rally.domain.recap.UserReviewDomainService.ReviewSummaryDTO;
import com.rally.domain.recap.enums.SetFormatEnum;
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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
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

    @Resource
    private RecapDomainService recapDomainService;

    @Resource
    private UserFollowDomainService userFollowDomainService;

    /** 战绩明细展示数量 */
    private static final int SET_SCORE_ITEM_LIMIT = 5;

    /** 战绩明细标题日期格式 */
    private static final DateTimeFormatter SET_TITLE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

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
        items.add(buildOneScoreItem("信誉分", "reputation", profileData.getReputationScore(), SystemConfigKey.SCORE_INFO_REPUTATION, 1));
        items.add(buildOneScoreItem("可信度", "credibility", profileData.getCredibilityScore(), SystemConfigKey.SCORE_INFO_CREDIBILITY, 2));
        items.add(buildOneScoreItem("校准度", "calibration", profileData.getCalibrationScore(), SystemConfigKey.SCORE_INFO_CALIBRATION, 3));
        return items;
    }

    /** 构建单个评分明细项 */
    private ScoreItemDTO buildOneScoreItem(String name, String key, BigDecimal score, SystemConfigKey infoConfigKey, Integer sort) {
        return new ScoreItemDTO()
                .setName(name)
                .setKey(key)
                .setValue(score != null ? score.toPlainString() : "0")
                .setMaxValue("1500")
                .setInfo(SystemConfig.getString(infoConfigKey.getKey(), infoConfigKey.getDefaultValue()))
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
        List<MyProfileSetScoreDTO.SetItem> setItems = records.stream()
                .limit(SET_SCORE_ITEM_LIMIT)
                .map(record -> buildSetItem(userId, record))
                .collect(Collectors.toList());
        return new MyProfileSetScoreDTO()
                .setTotal((long) records.size())
                .setSingleCount(singleCount)
                .setDoubleCount(doubleCount)
                .setSetItems(setItems);
    }

    /** 构建单条战绩明细：根据当前用户所在阵营与比分判断胜负 */
    private MyProfileSetScoreDTO.SetItem buildSetItem(String userId, ScoreRecordData record) {
        boolean userInSideA = userId.equals(record.getSideAPlayer1()) || userId.equals(record.getSideAPlayer2());
        boolean sideAWin = resolveSideAWin(record);
        ResultTypeEnum resultType = (userInSideA == sideAWin) ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;
        String matchTypeLabel = record.getMatchType() == MatchTypeEnum.DOUBLE ? "双打" : "单打";
        String title = record.getMeetupDate().format(SET_TITLE_DATE_FORMATTER) + " " + matchTypeLabel + " " + buildSetFormatLabel(record);
        return new MyProfileSetScoreDTO.SetItem()
                .setTitle(title)
                .setResultType(resultType)
                .setMatchType(record.getMatchType())
                .setSideAPlayer1AvatarUrl(record.getSideAPlayer1Avatar())
                .setSideAPlayer2AvatarUrl(record.getSideAPlayer2Avatar())
                .setSideAScore(String.valueOf(record.getSideAScore()))
                .setSideBPlayer1AvatarUrl(record.getSideBPlayer1Avatar())
                .setSideBPlayer2AvatarUrl(record.getSideBPlayer2Avatar())
                .setSideBScore(String.valueOf(record.getSideBScore()));
    }

    /** 判定 A 侧是否胜出：常规比分相同（如 6:6）时按抢七比分判定 */
    private boolean resolveSideAWin(ScoreRecordData record) {
        Integer sideAScore = record.getSideAScore();
        Integer sideBScore = record.getSideBScore();
        if (sideAScore.equals(sideBScore) && record.getSideATiebreakScore() != null && record.getSideBTiebreakScore() != null) {
            return record.getSideATiebreakScore() > record.getSideBTiebreakScore();
        }
        return sideAScore > sideBScore;
    }

    /** 构建赛制标签：常规局按本盘最大局数显示「X局」，抢七统一显示「抢分」 */
    private String buildSetFormatLabel(ScoreRecordData record) {
        if (record.getSetFormat() == SetFormatEnum.TIEBREAK) {
            return "抢分";
        }
        int games = Math.max(record.getSideAScore(), record.getSideBScore()) - 1;
        return games + "局";
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
                        .setTitle(video.getTitle()))
                .collect(Collectors.toList()));
        videoDTO.setMaxCount(SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_COUNT.getKey(), Integer.parseInt(SystemConfigKey.USER_VIDEO_MAX_COUNT.getDefaultValue())));
        videoDTO.setMaxSizeMb(SystemConfig.getInt(SystemConfigKey.USER_VIDEO_MAX_SIZE_MB.getKey(), Integer.parseInt(SystemConfigKey.USER_VIDEO_MAX_SIZE_MB.getDefaultValue())));
        return videoDTO;
    }
}
