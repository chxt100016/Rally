package com.rally.meetup;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService;
import com.rally.domain.meetup.service.MeetupQueryDomainService.NearbyMeetupData;
import com.rally.domain.meetup.service.MeetupQueryDomainService.QueryResult;
import com.rally.domain.recap.model.Recap;
import com.rally.domain.recap.model.RecapDetailVO;
import com.rally.domain.recap.service.RecapDomainService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.domain.utils.Assert;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 约球读流程编排：列表、详情
 * 仅做流程编排，核心查询逻辑委托给 MeetupQueryDomainService
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryAppService {

    private final RegistrationGateway registrationGateway;
    private final UserGateway userGateway;
    private final TennisProfileGateway tennisProfileGateway;
    private final MeetupDomainService meetupDomainService;
    private final MeetupQueryDomainService meetupQueryDomainService;
    private final RecapDomainService recapDomainService;

    private static final MeetupAppConvertMapper MAPPER = MeetupAppConvertMapper.INSTANCE;

    /**
     * 约球列表
     */
    public PageDTO<MeetupCardVO> list(MeetupListQuery query) {
        String currentUserId = UserContext.get();

        return switch (query.getSort()) {
            case DISTANCE -> listByDistance(query, currentUserId);
            case TIME -> listByTime(query, currentUserId);
            default -> null;
        };

    }

    /**
     * 按时间排序的列表
     */
    private PageDTO<MeetupCardVO> listByTime(MeetupListQuery query, String currentUserId) {
        // 1. 调用领域服务查询
        QueryResult<MeetupData> result = meetupQueryDomainService.listByTime(query);

        // 2. 分页
        List<MeetupData> pageData = paginate(result.getList(), query.getPageNo(), query.getPageSize());

        // 3. 转换为 VO
        List<MeetupCardVO> cardVOs = pageData.stream()
                .map(m -> buildMeetupCardVO(m, currentUserId))
                .collect(Collectors.toList());

        return new PageDTO<>(cardVOs, result.getTotal(), hasMore(result.getList(), query.getPageNo(), query.getPageSize()));
    }

    /**
     * 按距离排序的列表
     */
    private PageDTO<MeetupCardVO> listByDistance(MeetupListQuery query, String currentUserId) {
        // 距离排序必须提供经纬度
        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);

        // 1. 调用领域服务查询
        QueryResult<NearbyMeetupData> result = meetupQueryDomainService.listByDistance(query);

        // 2. 分页
        List<NearbyMeetupData> pageData = paginate(result.getList(), query.getPageNo(), query.getPageSize());

        // 3. 转换为 VO（含距离信息）
        List<MeetupCardVO> cardVOs = pageData.stream()
                .map(item -> {
                    MeetupCardVO card = buildMeetupCardVO(item.getMeetupData(), currentUserId);
                    card.setDistanceMeters(item.getDistanceMeters());
                    return card;
                })
                .collect(Collectors.toList());

        return new PageDTO<>(cardVOs, result.getTotal(), hasMore(result.getList(), query.getPageNo(), query.getPageSize()));
    }

    /**
     * 约球详情
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

        // 6. 填充参与者列表
        // TODO: 从 waitlist 查询参与者

        // 7. 活动已结束时，查询赛后收集数据
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
     * 查询赛后收集详情（VO，供 MeetupQueryService 合并使用）
     */
    public RecapDetailVO detail2(String meetupId) {
        String userId = UserContext.get();

        // 1. 加载聚合根（含业务校验）
        Recap recap = recapDomainService.get(userId, meetupId);

        // 2. 领域服务构建 VO
        return recapDomainService.detail(recap);
    }

    /**
     * 构建 MeetupCardVO（从 DTO 转换并填充计算字段）
     */
    private MeetupCardVO buildMeetupCardVO(MeetupData data, String currentUserId) {
        // 1. 转换基础字段
        MeetupCardDTO dto = MAPPER.toMeetupCardDTO(data);
        MeetupCardVO card = new MeetupCardVO();
        // 复制 DTO 字段到 VO
        copyDTOFields(dto, card);

        Meetup meetup = new Meetup(data);

        // 2. 计算每人费用（委托领域服务）
        card.setPerPersonCost(meetupDomainService.calculatePerPersonCost(data));

        // 3. 计算 actionState（委托领域服务，含报名记录上下文）
        RegistrationData userRegistration = registrationGateway.findActiveByMeetupAndUser(data.getBizId(), currentUserId);
        int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);
        card.setActionState(meetupDomainService.calculateActionState(meetup, currentUserId, lockMinutes, userRegistration));

        // 4. 填充发布者信息
        fillCreatorInfo(card, data.getCreatorId());

        return card;
    }

    /**
     * 复制 DTO 字段到 VO
     */
    private void copyDTOFields(MeetupCardDTO dto, MeetupCardVO vo) {
        vo.setMeetupId(dto.getMeetupId());
        vo.setTitle(dto.getTitle());
        vo.setMatchType(dto.getMatchType());
        vo.setMaxPlayers(dto.getMaxPlayers());
        vo.setCurrentPlayers(dto.getCurrentPlayers());
        vo.setStartTime(dto.getStartTime());
        vo.setEndTime(dto.getEndTime());
        vo.setDuration(dto.getDuration());
        vo.setCourtName(dto.getCourtName());
        vo.setCourtAddress(dto.getCourtAddress());
        vo.setLevelMode(dto.getLevelMode());
        vo.setLevelValue(dto.getLevelValue());
        vo.setGenderLimit(dto.getGenderLimit());
        vo.setJoinMode(dto.getJoinMode());
        vo.setStatus(dto.getStatus());
    }

    /**
     * 填充发布者信息到 MeetupVO
     */
    private void fillCreatorInfo(MeetupVO vo, String creatorId) {
        UserData creator = userGateway.findByUserId(creatorId).orElse(null);
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
            vo.setCreatorAvatarUrl(creator.getAvatarUrl());
        }
        TennisProfileData creatorProfile = tennisProfileGateway.findByUserId(creatorId).orElse(null);
        if (creatorProfile != null) {
            vo.setCreatorNtrp(creatorProfile.getNtrpScore());
        }
    }

    /**
     * 填充发布者信息到 MeetupCardVO
     */
    private void fillCreatorInfo(MeetupCardVO card, String creatorId) {
        UserData creator = userGateway.findByUserId(creatorId).orElse(null);
        if (creator != null) {
            card.setCreatorNickname(creator.getNickname());
            card.setCreatorAvatarUrl(creator.getAvatarUrl());
        }
    }

    /**
     * 分页处理
     */
    private <T> List<T> paginate(List<T> list, Integer pageNo, Integer pageSize) {
        int page = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, list.size());

        return fromIndex < list.size() ? list.subList(fromIndex, toIndex) : new ArrayList<>();
    }

    /**
     * 判断是否有更多数据
     */
    private <T> boolean hasMore(List<T> list, Integer pageNo, Integer pageSize) {
        int page = pageNo != null ? pageNo : 1;
        int size = pageSize != null ? pageSize : 10;
        int toIndex = page * size;
        return toIndex < list.size();
    }
}
