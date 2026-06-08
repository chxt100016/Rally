package com.rally.meetup;

import com.rally.utils.UserContext;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.domain.recap.model.Recap;
import com.rally.domain.recap.model.RecapDTO;
import com.rally.domain.recap.service.RecapDomainService;
import com.rally.domain.review.model.ReviewData;
import com.rally.domain.review.model.ScoreRecordData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileService;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 约球查询应用服务
 * 编排领域服务完成查询场景
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MeetupQueryAppService {

    private final MeetupQueryDomainService meetupQueryDomainService;
    private final MeetupDomainService meetupDomainService;
    private final MeetupGateway meetupGateway;
    private final UserProfileService userProfileService;
    private final RecapDomainService recapDomainService;

    private static final MeetupAppConvertMapper MAPPER = MeetupAppConvertMapper.INSTANCE;

    /**
     * 约球列表查询（按时间/距离）
     */
    public PageDTO<MeetupCardDTO> queryMeetupList(MeetupListCmd query) {
        return switch (query.getSort()) {
            case DISTANCE -> meetupQueryDomainService.listByDistance(query);
            case TIME -> meetupQueryDomainService.listByTime(query);
            default -> null;
        };
    }

    /**
     * 查询约球详情（重构后返回 MeetupDetailDTO）
     */
    public MeetupDetailDTO detail(String meetupId) {
        String currentUserId = UserContext.get();

        // 1 获取聚合根（含报名记录）
        Meetup meetup = meetupDomainService.getAggregate(meetupId);
        MeetupData data = meetup.getData();

        // 2 批量查询所有参与者用户信息（创建者 + 已批准报名者，去重）
        List<String> allUserIds = meetup.getAllParticipantUserIds();
        Map<String, UserProfile> profileMap = userProfileService.listProfiles(allUserIds);

        MeetupDetailDTO detailDTO = new MeetupDetailDTO();
        detailDTO.setMeetup(MAPPER.toMeetupDTO(data));
        detailDTO.setActionState(meetup.getActionState(currentUserId));
        detailDTO.setCreator(buildCreatorDTO(data.getCreatorId(), profileMap));
        detailDTO.setParticipants(buildParticipantVOList(allUserIds, data.getCreatorId(), profileMap));
        if (data.getStatus() == MeetupStatusEnum.FINISHED) {
            detailDTO.setRecap(buildRecap(meetupId));
        }
        return detailDTO;
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
            creator.setAvatarUrl(profile.getUser().getAvatarUrl());
        }
        if (profile != null && profile.getProfile() != null) {
            creator.setNtrpScore(profile.getProfile().getNtrpScore());
        }
        creator.setPublishMeetupCount(meetupGateway.countByCreatorId(creatorId));
        return creator;
    }

    /**
     * 构建参与者列表（排除创建人）
     */
    private List<ParticipantVO> buildParticipantVOList(List<String> allUserIds, String creatorId,
                                                       Map<String, UserProfile> profileMap) {
        return allUserIds.stream()
                .filter(uid -> !uid.equals(creatorId))
                .map(uid -> {
                    ParticipantVO vo = new ParticipantVO();
                    vo.setUserId(uid);
                    UserProfile profile = profileMap.get(uid);
                    if (profile != null && profile.getUser() != null) {
                        vo.setNickname(profile.getUser().getNickname());
                        vo.setAvatarUrl(profile.getUser().getAvatarUrl());
                    }
                    if (profile != null && profile.getProfile() != null) {
                        vo.setNtrpScore(profile.getProfile().getNtrpScore());
                    }
                    return vo;
                })
                .toList();
    }

    /**
     * 构建赛后收集详情 VO
     */
    public RecapDTO buildRecap(String meetupId) {
        Recap recap = recapDomainService.get(UserContext.get(), meetupId);

        // 当前用户已填评价（按 toUser 分组）
        Map<String, List<RecapDTO.ReviewItem>> myReviewsMap = new LinkedHashMap<>();
        for (ReviewData review : recap.getMyReviews().values()) {
            RecapDTO.ReviewItem item = new RecapDTO.ReviewItem();
            item.setToUserId(review.getToUserId());
            item.setType(review.getReviewType().name());
            item.setValue(review.getReviewValue());
            myReviewsMap.computeIfAbsent(review.getToUserId(), k -> new ArrayList<>()).add(item);
        }

        // 比分
        List<RecapDTO.ScoreItem> scoreItems = recap.getScoreBoard() != null && recap.getScoreBoard().getScores() != null
                ? recap.getScoreBoard().getScores().stream().map(this::toScoreItem).toList()
                : List.of();

        RecapDTO dto = new RecapDTO();
        dto.setMyReviews(myReviewsMap);
        dto.setScores(scoreItems);
        dto.setScoreVersion(recap.getScoreBoard() != null ? recap.getScoreBoard().getVersion() : 0);
        dto.setScoreFilled(!scoreItems.isEmpty());
        return dto;
    }

    private RecapDTO.ScoreItem toScoreItem(ScoreRecordData data) {
        RecapDTO.ScoreItem item = new RecapDTO.ScoreItem();
        item.setBizId(data.getBizId());
        item.setSetNum(data.getSetNumber());
        item.setSetFormat(data.getSetFormat() != null ? data.getSetFormat().name() : null);
        item.setSideAPlayer1(data.getSideAPlayer1());
        item.setSideAPlayer2(data.getSideAPlayer2());
        item.setSideBPlayer1(data.getSideBPlayer1());
        item.setSideBPlayer2(data.getSideBPlayer2());
        item.setSideAScore(data.getSideAScore());
        item.setSideBScore(data.getSideBScore());
        return item;
    }
}
