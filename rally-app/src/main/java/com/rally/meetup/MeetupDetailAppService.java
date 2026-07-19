package com.rally.meetup;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.ActionStateEnum;
import com.rally.domain.meetup.enums.JoinRestrictionEnum;
import com.rally.domain.meetup.enums.MeetupRoleEnum;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.UserMeetupQueryDomainService;
import com.rally.domain.recap.model.RecapDTO;
import com.rally.domain.recap.model.ReviewData;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.service.ReviewDomainService;
import com.rally.domain.recap.service.ScoreDomainService;
import com.rally.domain.score.ProfileLevelManager;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.UserExtKeyEnum;
import com.rally.domain.user.model.UserExtData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserExtDomainService;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import com.rally.utils.SunUtils;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
    private final UserMeetupQueryDomainService userMeetupQueryDomainService;
    private final UserProfileDomainService userProfileDomainService;
    private final ReviewDomainService reviewDomainService;
    private final ScoreDomainService scoreDomainService;
    private final ChatDomainService chatDomainService;
    private final UserExtDomainService userExtDomainService;


    /**
     * 查询约球详情（重构后返回 MeetupDetailDTO）
     */
    public MeetupDetailDTO detail(String meetupId, String shareUserId) {

        String currentUserId = UserContext.get();
        // 通过分享页面进入
        if (shareUserId != null) {
            log.info("from share, userId: {}, shareUserId:{}", currentUserId, shareUserId);
        }

        // 1 获取聚合根 （含报名记录）
        Meetup meetup = meetupDomainService.get(meetupId);

        // 2 按视角获取参与者报名记录，批量查询用户信息（creatorId 兜底，确保创建人信息可查）
        List<RegistrationData> participants = meetup.getParticipants(currentUserId);
        List<String> allQueryUserIds = participants.stream().map(RegistrationData::getUserId).collect(Collectors.toCollection(ArrayList::new));
        allQueryUserIds.add(meetup.getCreatorId());
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(allQueryUserIds);

        ActionStateEnum actionState = meetup.getActionState(currentUserId);
        MeetupDetailDTO detail = new MeetupDetailDTO()
                .setMeetup(MeetupAppConvertMapper.INSTANCE.toMeetupDTO(meetup.getData()))
                .setActionState(actionState)
                .setWeather(buildWeather(meetup))
                .setCreator(buildCreatorDTO(meetup.getCreatorId(), profileMap))
                .setParticipants(buildParticipantsView(meetup, currentUserId, participants, profileMap))
                .setRecap(meetup.canReview() ? buildRecap(meetup) : null)
                .setUnreadCount(meetup.canChat(currentUserId) ? chatDomainService.getUnreadCount(meetupId, currentUserId) : null)
                .setPayment(buildPaymentView(meetup, currentUserId));

        // 仅未报名场景计算准入限制，决定 报名按钮是否可点
        if (actionState == ActionStateEnum.JOIN_DIRECT || actionState == ActionStateEnum.APPLY_APPROVAL) {
            List<JoinRestrictionEnum> restrictions = meetup.collectJoinRestrictions(userProfileDomainService.get(currentUserId));
            detail.setRestrictions(restrictions).setJoinable(restrictions.isEmpty());
        }
        return detail;

    }

    /**
     * 组装支付视图子对象。从 costItems 构建支付信息。
     * 收款人视角：返回收款码 URL；付款人视角：返回需支付金额；陌生人：未加入活动的用户。
     * 根据活动状态决定计算人数：未开始用最大人数，已开始/结束用当前人数。
     */
    private PaymentDTO buildPaymentView(Meetup meetup, String currentUserId) {
        MeetupData data = meetup.getData();
        if (data.getCostItems() == null || data.getCostItems().isEmpty()) {
            return null;
        }
        PaymentDTO payment = new PaymentDTO();
        payment.setCostItems(data.getCostItems());
        boolean isCreator = meetup.isCreator(currentUserId);
        boolean isParticipant = meetup.isParticipant(currentUserId);
        if (isCreator) {
            payment.setUserRole(PaymentDTO.UserRoleEnum.COLLECTOR);
        } else if (isParticipant) {
            payment.setUserRole(PaymentDTO.UserRoleEnum.PAYER);
        } else {
            payment.setUserRole(PaymentDTO.UserRoleEnum.STRANGER);
        }
        MeetupStatusEnum realStatus = meetup.getRealStatus();
        int calculatedPlayerCount;
        if (realStatus == MeetupStatusEnum.OPEN) {
            calculatedPlayerCount = data.getMaxPlayers();
        } else {
            calculatedPlayerCount = meetup.countApprovedPlayers();
        }
        payment.setCalculatedPlayerCount(calculatedPlayerCount);
        int totalAmount = data.getCostItems().stream().mapToInt(CostItem::getTotalAmount).sum();
        payment.setTotalAmount(totalAmount);
        if (data.getCostData() != null && data.getCostData().getHourlyAllocations() != null && !data.getCostData().getHourlyAllocations().isEmpty()) {
            List<HourlyAllocation> hourlyAllocations = data.getCostData().getHourlyAllocations();
            payment.setAllocationMode(PaymentDTO.AllocationModeEnum.HOURLY);
            payment.setHourlyAllocations(hourlyAllocations);
            int currentUserAmount = calculateUserAmountByHourly(totalAmount, data.getDuration(), hourlyAllocations, currentUserId);
            payment.setCurrentUserAmount(currentUserAmount);
            payment.setAllocationDesc(buildAllocationDesc(hourlyAllocations, currentUserId));
        } else {
            payment.setAllocationMode(PaymentDTO.AllocationModeEnum.AVERAGE);
            int amountPerPerson = calculatedPlayerCount > 0 ? totalAmount / calculatedPlayerCount : 0;
            payment.setCurrentUserAmount(amountPerPerson);
        }
        String creatorId = meetup.getCreatorId();
        UserExtData paymentCodeData = userExtDomainService.get(creatorId, UserExtKeyEnum.WECHAT_PAYMENT_CODE.getKey());
        if (paymentCodeData != null) {
            payment.setPaymentCodeUrl(QiniuConfiguration.buildSignedUrl(paymentCodeData.getExtValue()));
        }
        return payment;
    }

    private int calculateUserAmountByHourly(int totalAmount, BigDecimal duration, List<HourlyAllocation> hourlyAllocations, String userId) {
        BigDecimal hourlyRate = new BigDecimal(totalAmount).divide(duration, 2, BigDecimal.ROUND_HALF_UP);
        BigDecimal userAmount = BigDecimal.ZERO;
        for (HourlyAllocation allocation : hourlyAllocations) {
            if (allocation.getUserIds() != null && allocation.getUserIds().contains(userId)) {
                BigDecimal periodAmount = hourlyRate.multiply(allocation.getDuration());
                BigDecimal userCount = new BigDecimal(allocation.getUserIds().size());
                BigDecimal userShare = periodAmount.divide(userCount, 2, BigDecimal.ROUND_HALF_UP);
                userAmount = userAmount.add(userShare);
            }
        }
        return userAmount.intValue();
    }

    /**
     * 构建当前用户的人时分摊详情文案，如"4人2小时、3人1小时"。
     * 按参与人数分组，累加相同人数下的时长。
     */
    private String buildAllocationDesc(List<HourlyAllocation> hourlyAllocations, String userId) {
        Map<Integer, BigDecimal> durationByPlayerCount = new LinkedHashMap<>();
        for (HourlyAllocation allocation : hourlyAllocations) {
            if (allocation.getUserIds() != null && allocation.getUserIds().contains(userId)) {
                int playerCount = allocation.getUserIds().size();
                durationByPlayerCount.merge(playerCount, allocation.getDuration(), BigDecimal::add);
            }
        }
        return durationByPlayerCount.entrySet().stream()
                .map(entry -> entry.getKey() + "人" + entry.getValue().stripTrailingZeros().toPlainString() + "小时")
                .collect(Collectors.joining("、"));
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
        creator.setPublishMeetupCount(userMeetupQueryDomainService.countMyPublish(creatorId));
        creator.setGender(profile.getUser().getGender());
        return creator;
    }

    /**
     * 构建参与者视图（含当前用户角色，统一方法，展示报名状态）
     * @param participants 领域层按视角返回的参与者报名记录
     */
    private ParticipantsViewDTO buildParticipantsView(Meetup meetup, String currentUserId, List<RegistrationData> participants, Map<String, UserProfile> profileMap) {
        List<ParticipantDTO> list = participants.stream()
                .map(registration -> toParticipantVO(registration, profileMap))
                .toList();
        MeetupRoleEnum userRole = meetup.isCreator(currentUserId) ? MeetupRoleEnum.CREATOR : MeetupRoleEnum.NOT_CREATOR;
        return new ParticipantsViewDTO().setUserRole(userRole).setList(list);
    }

    /**
     * 构建单个参与者 VO
     */
    private ParticipantDTO toParticipantVO(RegistrationData registration, Map<String, UserProfile> profileMap) {
        String uid = registration.getUserId();
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
        vo.setStatus(registration.getStatus());
        vo.setRegistrationId(registration.getBizId());
        vo.setApplyTime(registration.getCreateTime());
        return vo;
    }

    /**
     * 构建赛后收集详情 VO
     */
    public RecapDTO buildRecap(Meetup meetup) {
        String currentUserId = UserContext.get();

        // 1. 查询当前用户已提交的评价，按 toUserId 分组
        List<ReviewData> myReviews = reviewDomainService.listReviewsByMeetupAndFrom(meetup.getMeetupId(), currentUserId);
        Map<String, List<RecapDTO.ReviewItem>> reviewMap = myReviews.stream()
                .collect(Collectors.groupingBy(
                        ReviewData::getToUserId,
                        Collectors.mapping(MeetupAppConvertMapper.INSTANCE::toReviewItem, Collectors.toList())
                ));

        // 2. 查询该活动的比分记录
        List<ScoreRecordData> scoreRecords = scoreDomainService.listScoresByMeetup(meetup.getMeetupId());

        // 3. 组装 RecapDTO
        RecapDTO recap = new RecapDTO();
        recap.setWaitlistIds(meetup.getReviewWaitlistIds(currentUserId));
        recap.setMyReviews(reviewMap);
        recap.setScores(MeetupAppConvertMapper.INSTANCE.toScoreItemList(scoreRecords));
        recap.setScoreFilled(!scoreRecords.isEmpty());
        String defaultTag = SystemConfig.getString(SystemConfigKey.REVIEW_DEFAULT_TAGS.getKey());
        if (StringUtils.isNotBlank(defaultTag)) {
            recap.setDefaultTags(List.of(defaultTag.split(",")));
        }

        return recap;
    }


}
