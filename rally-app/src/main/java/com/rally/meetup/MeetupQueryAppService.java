package com.rally.meetup;

import com.rally.cache.UserContext;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.domain.recap.model.Recap;
import com.rally.domain.recap.model.RecapDetailVO;
import com.rally.domain.recap.service.RecapDomainService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    private final RegistrationGateway registrationGateway;
    private final UserGateway userGateway;
    private final TennisProfileGateway tennisProfileGateway;
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
     * 查询约球详情
     */
    public MeetupVO detail(String meetupId) {
        String currentUserId = UserContext.get();

        // 1. 获取核心数据
        MeetupData data = meetupQueryDomainService.getDetail(meetupId);
        MeetupVO vo = MAPPER.toMeetupVO(data);
        Meetup meetup = new Meetup(data);

        // 2. 计算每人费用（委托领域服务）
        vo.setPerPersonCost(meetupDomainService.calculatePerPersonCost(data));

        // 3. 计算 actionState（委托领域服务，含报名记录上下文）
        RegistrationData userRegistration = registrationGateway.findActiveByMeetupAndUser(meetupId, currentUserId);
        int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);
        vo.setActionState(meetupDomainService.calculateActionState(meetup, currentUserId, lockMinutes, userRegistration));

        // 4. 计算 quitWillPenalize（委托领域服务）
        vo.setQuitWillPenalize(meetupDomainService.calculateQuitWillPenalize(data, currentUserId));

        // 5. 填充发布者信息
        fillCreatorInfo(vo, data.getCreatorId());

        // 6. 活动已结束时，查询赛后收集数据
        if (data.getStatus() == MeetupStatusEnum.FINISHED) {
            try {
                RecapDetailVO recap = this.detail2(meetupId);
                vo.setRecap(recap);
            } catch (Exception e) {
                log.warn("查询赛后收集失败，meetupId={}", meetupId, e);
            }
        }

        return vo;
    }

    /**
     * 查询赛后收集详情
     */
    public RecapDetailVO detail2(String meetupId) {
        String userId = UserContext.get();

        // 1. 加载聚合根（含业务校验）
        Recap recap = recapDomainService.get(userId, meetupId);

        // 2. 领域服务构建 VO
        return recapDomainService.detail(recap);
    }

    /**
     * 填充发布者信息到 MeetupVO
     */
    private void fillCreatorInfo(MeetupVO vo, String creatorId) {
        if (creatorId == null) {
            return;
        }
        UserData user = userGateway.findByUserId(creatorId).orElse(null);
        if (user != null) {
            vo.setCreatorNickname(user.getNickname());
            vo.setCreatorAvatarUrl(user.getAvatarUrl());
        }
        TennisProfileData profile = tennisProfileGateway.findByUserId(creatorId).orElse(null);
        if (profile != null) {
            vo.setCreatorNtrp(profile.getNtrpScore());
        }
    }
}
