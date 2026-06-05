package com.rally.meetup;

import com.rally.cache.UserContext;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.gateway.UserGateway;
import com.rally.domain.user.model.TennisProfileData;
import com.rally.domain.user.model.UserData;
import com.rally.meetup.convert.MeetupAppConvertMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 约球读流程：列表、详情、actionState 计算、城市列表
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryService {

    private final MeetupGateway meetupGateway;
    private final RegistrationGateway registrationGateway;
    private final NearbyGateway nearbyGateway;
    private final UserGateway userGateway;
    private final TennisProfileGateway tennisProfileGateway;
    private final MeetupDomainService meetupDomainService;

    private static final MeetupAppConvertMapper MAPPER = MeetupAppConvertMapper.INSTANCE;

    /**
     * 约球列表
     */
    public PageVO<MeetupCardVO> list(MeetupListQuery query) {
        String currentUserId = UserContext.get();

        // 根据排序方式选择不同的查询策略
        if ("distance".equals(query.getSort()) && query.getLng() != null && query.getLat() != null) {
            return listByDistance(query, currentUserId);
        } else {
            return listByTime(query, currentUserId);
        }
    }

    /**
     * 按时间排序的列表
     */
    private PageVO<MeetupCardVO> listByTime(MeetupListQuery query, String currentUserId) {
        // 构建查询条件
        List<MeetupData> allMeetups = meetupGateway.findByCityCodeAndStatus(
                query.getCityCode(),
                List.of(MeetupStatusEnum.OPEN.name(), MeetupStatusEnum.FULL.name()));

        // 过滤已结束的
        allMeetups = allMeetups.stream()
                .filter(m -> m.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        // 应用筛选条件
        allMeetups = applyFilters(allMeetups, query);

        // 排序
        allMeetups.sort(Comparator.comparing(MeetupData::getStartTime));

        // 分页
        int pageNo = query.getPageNo() != null ? query.getPageNo() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        int fromIndex = (pageNo - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, allMeetups.size());

        List<MeetupData> pageData = fromIndex < allMeetups.size()
                ? allMeetups.subList(fromIndex, toIndex)
                : new ArrayList<>();

        // 转换为 VO
        List<MeetupCardVO> cardVOs = pageData.stream()
                .map(m -> buildMeetupCardVO(m, currentUserId))
                .collect(Collectors.toList());

        return new PageVO<>(cardVOs, (long) allMeetups.size(), toIndex < allMeetups.size());
    }

    /**
     * 按距离排序的列表（GEO 特化分页）
     */
    private PageVO<MeetupCardVO> listByDistance(MeetupListQuery query, String currentUserId) {
        double radiusMeters = query.getRadiusKm() != null
                ? query.getRadiusKm().multiply(new BigDecimal("1000")).doubleValue()
                : 10000; // 默认 10km

        // 1. GEO 查询候选
        List<NearbyResult> nearbyResults = nearbyGateway.searchByRadius(
                query.getCityCode(), query.getLng(), query.getLat(), radiusMeters);

        if (nearbyResults.isEmpty()) {
            return new PageVO<>(new ArrayList<>(), 0L, false);
        }

        // 2. 批量查询详情
        List<String> meetupIds = nearbyResults.stream()
                .map(NearbyResult::getMeetupId)
                .collect(Collectors.toList());
        Map<String, Double> distanceMap = nearbyResults.stream()
                .collect(Collectors.toMap(NearbyResult::getMeetupId, NearbyResult::getDistanceMeters));

        List<MeetupData> meetups = meetupGateway.findByBizIds(meetupIds);

        // 3. 过滤状态和筛选条件
        meetups = meetups.stream()
                .filter(m -> (m.getStatus() == MeetupStatusEnum.OPEN || m.getStatus() == MeetupStatusEnum.FULL)
                        && m.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        meetups = applyFilters(meetups, query);

        // 4. 按距离排序
        meetups.sort(Comparator.comparingDouble(m ->
                distanceMap.getOrDefault(m.getBizId(), Double.MAX_VALUE)));

        // 5. 分页
        int pageNo = query.getPageNo() != null ? query.getPageNo() : 1;
        int pageSize = query.getPageSize() != null ? query.getPageSize() : 10;
        int fromIndex = (pageNo - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, meetups.size());

        List<MeetupData> pageData = fromIndex < meetups.size()
                ? meetups.subList(fromIndex, toIndex)
                : new ArrayList<>();

        // 6. 转换为 VO
        List<MeetupCardVO> cardVOs = pageData.stream()
                .map(m -> {
                    MeetupCardVO card = buildMeetupCardVO(m, currentUserId);
                    card.setDistanceMeters(distanceMap.get(m.getBizId()));
                    return card;
                })
                .collect(Collectors.toList());

        return new PageVO<>(cardVOs, (long) meetups.size(), toIndex < meetups.size());
    }

    /**
     * 应用筛选条件
     */
    private List<MeetupData> applyFilters(List<MeetupData> meetups, MeetupListQuery query) {
        return meetups.stream()
                .filter(m -> {
                    // 类型筛选
                    if (query.getMatchType() != null && m.getMatchType() != query.getMatchType()) {
                        return false;
                    }
                    // 时间范围筛选
                    if (query.getStartFrom() != null && m.getStartTime().isBefore(query.getStartFrom())) {
                        return false;
                    }
                    if (query.getStartTo() != null && m.getStartTime().isAfter(query.getStartTo())) {
                        return false;
                    }
                    // 水平筛选
                    if (query.getLevelMin() != null || query.getLevelMax() != null) {
                        if (!meetupDomainService.matchLevel(m, query.getLevelMin(), query.getLevelMax())) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    /**
     * 约球详情
     */
    public MeetupVO detail(String meetupId) {
        String currentUserId = UserContext.get();

        MeetupData data = meetupGateway.findByBizId(meetupId);
        if (data == null) {
            throw new BusinessException(BizErrorCode.MEETUP_NOT_FOUND);
        }

        MeetupVO vo = MAPPER.toMeetupVO(data);
        Meetup meetup = new Meetup(data);

        // 计算每人费用（委托领域服务）
        vo.setPerPersonCost(meetupDomainService.calculatePerPersonCost(data));

        // 计算 actionState（委托领域服务，含报名记录上下文）
        RegistrationData userRegistration = registrationGateway.findActiveByMeetupAndUser(meetupId, currentUserId);
        int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);
        vo.setActionState(meetupDomainService.calculateActionState(meetup, currentUserId, lockMinutes, userRegistration));

        // 计算 quitWillPenalize（委托领域服务）
        vo.setQuitWillPenalize(meetupDomainService.calculateQuitWillPenalize(data, currentUserId));

        // 填充发布者信息
        UserData creator = userGateway.findByUserId(data.getCreatorId()).orElse(null);
        if (creator != null) {
            vo.setCreatorNickname(creator.getNickname());
            vo.setCreatorAvatarUrl(creator.getAvatarUrl());
        }
        TennisProfileData creatorProfile = tennisProfileGateway.findByUserId(data.getCreatorId()).orElse(null);
        if (creatorProfile != null) {
            vo.setCreatorNtrp(creatorProfile.getNtrpScore());
        }

        // 填充参与者列表
        // TODO: 从 waitlist 查询参与者

        return vo;
    }

    /**
     * 构建 MeetupCardVO
     */
    private MeetupCardVO buildMeetupCardVO(MeetupData data, String currentUserId) {
        MeetupCardVO card = MAPPER.toMeetupCardVO(data);
        Meetup meetup = new Meetup(data);

        // 计算每人费用（委托领域服务）
        card.setPerPersonCost(meetupDomainService.calculatePerPersonCost(data));

        // 计算 actionState（委托领域服务，含报名记录上下文）
        RegistrationData userRegistration = registrationGateway.findActiveByMeetupAndUser(data.getBizId(), currentUserId);
        int lockMinutes = SystemConfig.getInt("meetup.edit_lock_minutes_before_start", 60);
        card.setActionState(meetupDomainService.calculateActionState(meetup, currentUserId, lockMinutes, userRegistration));

        // 填充发布者信息
        UserData creator = userGateway.findByUserId(data.getCreatorId()).orElse(null);
        if (creator != null) {
            card.setCreatorNickname(creator.getNickname());
            card.setCreatorAvatarUrl(creator.getAvatarUrl());
        }

        return card;
    }
}
