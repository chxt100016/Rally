package com.rally.meetup;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.ActionStateEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.recap.model.RecapDTO;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.service.RecapDomainService;
import com.rally.domain.utils.GeoUtils;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import com.rally.utils.SunUtils;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 约球查询应用服务
 * 编排领域服务完成查询场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupDetailAppService {

    private final MeetupDomainService meetupDomainService;
    private final MeetupGateway meetupGateway;
    private final UserProfileDomainService userProfileDomainService;
    private final RecapDomainService recapDomainService;
    private final ChatDomainService chatDomainService;


    /**
     * 查询约球详情（重构后返回 MeetupDetailDTO）
     */
    public MeetupDetailDTO detail(String meetupId) {
        String currentUserId = UserContext.get();
        UserProfile currentUser = userProfileDomainService.get(currentUserId);
        currentUser.assertCompleted();

        // 1 获取聚合根（含报名记录）
        Meetup meetup = meetupDomainService.get(meetupId);

        // 2 按视角获取参与者列表，批量查询用户信息（creatorId 兜底，确保创建人信息可查）
        List<String> participantUserIds = meetup.getParticipantUserIds(currentUserId);
        List<String> allQueryUserIds = new ArrayList<>(participantUserIds);
        allQueryUserIds.add(meetup.getCreatorId());
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(allQueryUserIds);

        ActionStateEnum actionState = meetup.getActionState(currentUserId);
        return new MeetupDetailDTO()
                .setMeetup(MeetupAppConvertMapper.INSTANCE.toMeetupDTO(meetup.getData()))
                .setActionState(actionState)
                .setWeather(buildWeather(meetup))
                .setCreator(buildCreatorDTO(meetup.getCreatorId(), profileMap))
                .setParticipants(buildParticipantVOList(meetup, participantUserIds, profileMap))
                .setRecap(actionState == ActionStateEnum.FINISHED ? buildRecap(meetupId) : null)
                .setUnreadCount(actionState == ActionStateEnum.JOINED || actionState == ActionStateEnum.ONGOING_JOINED || actionState == ActionStateEnum.OWNER_EDITABLE || actionState == ActionStateEnum.OWNER_EDIT_LOCKED  ? chatDomainService.getUnreadCount(meetupId, currentUserId) : null);

    }

    private WeatherDTO buildWeather(Meetup meetup) {
        return new WeatherDTO()
                .setSunrise(SunUtils.sunrise(meetup.getData().getStartTime(), meetup.getData().getCourtLat(), meetup.getData().getCourtLng()))
                .setSunset(SunUtils.sunset(meetup.getData().getStartTime(), meetup.getData().getCourtLat(), meetup.getData().getCourtLng()))
                ;
    }


    /**
     * 构建创建人信息 DTO
     */
    private CreatorDTO buildCreatorDTO(String creatorId, Map<String, UserProfile> profileMap) {
        CreatorDTO creator = new CreatorDTO();
        if (creatorId == null) {
            return creator;
        }
        creator.setUserId(creatorId);
        UserProfile profile = profileMap.get(creatorId);
        if (profile != null && profile.getUser() != null) {
            creator.setNickname(profile.getUser().getNickname());
            creator.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
        }
        if (profile != null && profile.getProfile() != null) {
            creator.setNtrpScore(profile.getProfile().getNtrpScore());
        }
        creator.setPublishMeetupCount(meetupGateway.countByCreatorId(creatorId));
        return creator;
    }

    /**
     * 构建参与者列表（统一方法，展示报名状态）
     * @param participantUserIds 领域层按视角返回的参与者 userId 列表
     */
    private List<ParticipantDTO> buildParticipantVOList(Meetup meetup, List<String> participantUserIds, Map<String, UserProfile> profileMap) {
        return participantUserIds.stream()
                .map(uid -> toParticipantVO(uid, profileMap, resolveRegistration(meetup, uid)))
                .toList();
    }

    /** 查询用户在该约球中的报名记录（用于创建人视角展示状态与审批） */
    private RegistrationData resolveRegistration(Meetup meetup, String userId) {
        return meetup.getRegistrations().stream()
                .filter(r -> userId.equals(r.getUserId()))
                .findFirst().orElse(null);
    }

    /**
     * 构建单个参与者 VO
     */
    private ParticipantDTO toParticipantVO(String uid, Map<String, UserProfile> profileMap,
                                           RegistrationData registration) {
        ParticipantDTO vo = new ParticipantDTO();
        vo.setUserId(uid);
        UserProfile profile = profileMap.get(uid);
        if (profile != null && profile.getUser() != null) {
            vo.setNickname(profile.getUser().getNickname());
            vo.setAvatarUrl(QiniuConfiguration.buildSignedUrl(profile.getUser().getAvatarUrl()));
            vo.setGender(profile.getUser().getGender());
        }
        if (profile != null && profile.getProfile() != null) {
            vo.setNtrpScore(profile.getProfile().getNtrpScore());
            vo.setProfileLevel(ProfileLevelManager.calculate(profile.getProfile()));
        }
        if (registration != null) {
            vo.setStatus(registration.getStatus());
            vo.setRegistrationId(registration.getBizId());
            vo.setApplyTime(registration.getCreateTime());
        }
        return vo;
    }

    /**
     * 构建赛后收集详情 VO
     */
    public RecapDTO buildRecap(String meetupId) {
        String currentUserId = UserContext.get();

        // 1. 查询当前用户已提交的评价，按 toUserId 分组
        List<ReviewData> myReviews = recapDomainService.listReviewsByMeetupAndFrom(meetupId, currentUserId);
        Map<String, List<RecapDTO.ReviewItem>> reviewMap = myReviews.stream()
                .collect(Collectors.groupingBy(
                        ReviewData::getToUserId,
                        Collectors.mapping(MeetupAppConvertMapper.INSTANCE::toReviewItem, Collectors.toList())
                ));

        // 2. 查询该活动的比分记录
        List<ScoreRecordData> scoreRecords = recapDomainService.listScoresByMeetup(meetupId);

        // 3. 组装 RecapDTO
        RecapDTO recap = new RecapDTO();
        recap.setMyReviews(reviewMap);
        recap.setScores(MeetupAppConvertMapper.INSTANCE.toScoreItemList(scoreRecords));
        recap.setScoreFilled(!scoreRecords.isEmpty());
        return recap;
    }


}
