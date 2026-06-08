package com.rally.domain.meetup.service;

import com.rally.domain.meetup.enums.MeetupSortEnum;
import com.rally.domain.meetup.enums.MeetupStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.*;
import com.rally.domain.utils.Assert;
import com.rally.domain.auth.enums.BizErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 约球查询领域服务
 * 负责列表查询（按时间/距离）、详情查询等读操作的核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryDomainService {

    private final MeetupGateway meetupGateway;
    private final NearbyGateway nearbyGateway;
    private final MeetupDomainService meetupDomainService;

    /**
     * 判断是否按距离排序（含参数校验）
     * @param query 查询条件
     * @return true 表示按距离排序
     */
    public boolean isDistanceSort(MeetupListQuery query) {
        if (query.getSort() != MeetupSortEnum.DISTANCE) {
            return false;
        }

        return true;
    }

    /**
     * 按时间排序的列表查询
     * @param query 查询条件
     * @return 约球数据列表 + 总数（已排序、已过滤、未分页）
     */
    public QueryResult<MeetupData> listByTime(MeetupListQuery query) {
        // 1. 查询城市下活跃约球
        List<MeetupData> allMeetups = meetupGateway.findByCityCodeAndStatus(
                query.getCityCode(),
                List.of(MeetupStatusEnum.OPEN.name(), MeetupStatusEnum.FULL.name()));

        // 2. 过滤已结束的
        allMeetups = allMeetups.stream()
                .filter(m -> m.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());

        // 3. 应用筛选条件
        allMeetups = applyFilters(allMeetups, query);

        // 4. 按时间排序
        allMeetups.sort(Comparator.comparing(MeetupData::getStartTime));

        return new QueryResult<>(allMeetups, (long) allMeetups.size());
    }

    /**
     * 按距离排序的列表查询（GEO 特化）
     * @param query 查询条件（必须包含 lng/lat）
     * @return 约球数据列表 + 总数（已排序、已过滤、未分页），附带距离信息
     */
    public QueryResult<NearbyMeetupData> listByDistance(MeetupListQuery query) {
        // 1. 计算半径（默认 10km）
        double radiusMeters = query.getRadiusKm() != null
                ? query.getRadiusKm().multiply(new BigDecimal("1000")).doubleValue()
                : 10000;

        // 2. GEO 查询候选
        List<NearbyResult> nearbyResults = nearbyGateway.searchByRadius(
                query.getCityCode(), query.getLng(), query.getLat(), radiusMeters);

        if (nearbyResults.isEmpty()) {
            return new QueryResult<>(new ArrayList<>(), 0L);
        }

        // 3. 构建距离映射
        Map<String, Double> distanceMap = nearbyResults.stream()
                .collect(Collectors.toMap(NearbyResult::getMeetupId, NearbyResult::getDistanceMeters));

        // 4. 批量查询详情
        List<String> meetupIds = nearbyResults.stream()
                .map(NearbyResult::getMeetupId)
                .collect(Collectors.toList());
        List<MeetupData> meetups = meetupGateway.findByBizIds(meetupIds);

        // 5. 过滤状态和筛选条件
        meetups = meetups.stream()
                .filter(m -> (m.getStatus() == MeetupStatusEnum.OPEN || m.getStatus() == MeetupStatusEnum.FULL)
                        && m.getEndTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        meetups = applyFilters(meetups, query);

        // 6. 按距离排序
        meetups.sort(Comparator.comparingDouble(m ->
                distanceMap.getOrDefault(m.getBizId(), Double.MAX_VALUE)));

        // 7. 组装带距离的结果
        List<NearbyMeetupData> result = meetups.stream()
                .map(m -> new NearbyMeetupData(m, distanceMap.get(m.getBizId())))
                .collect(Collectors.toList());

        return new QueryResult<>(result, (long) meetups.size());
    }

    /**
     * 查询约球详情（核心数据）
     * @param meetupId 约球ID
     * @return 约球数据
     */
    public MeetupData getDetail(String meetupId) {
        MeetupData data = meetupGateway.findByBizId(meetupId);
        Assert.notNull(data, BizErrorCode.MEETUP_NOT_FOUND);
        return data;
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
     * 查询结果封装
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class QueryResult<T> {
        private List<T> list;
        private Long total;
    }

    /**
     * 带距离信息的约球数据
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class NearbyMeetupData {
        private MeetupData meetupData;
        private Double distanceMeters;
    }
}
