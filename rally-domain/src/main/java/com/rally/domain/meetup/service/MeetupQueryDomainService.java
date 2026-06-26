package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.gateway.NearbyRepository;
import com.rally.domain.meetup.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 约球查询领域服务
 * 负责广场列表查询（按时间/距离）等读操作的核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryDomainService {

    private final MeetupRepository meetupRepository;
    private final NearbyRepository nearbyRepository;
    private final MeetupQueryPlanner queryPlanner;

    /**
     * 按时间排序的列表查询（searchAfter 游标分页）
     * @param query 查询条件
     * @return 约球数据列表，最多 pageSize+1 条，供 app 层判断是否还有下一页
     */
    public List<MeetupData> listByTime(MeetupListCmd query) {
        MeetupListQueryParam param = queryPlanner.plan(query);
        if (param == null) {
            return List.of();
        }
        return meetupRepository.listAvailable(param);
    }

    /**
     * 按距离排序的列表查询（GEO 特化，searchAfter 游标分页）
     * 流程：一次 Redis 查询（距离+范围） → 数据库筛选 → 应用层排序 → 按游标取窗口
     * @param query 查询条件（必须包含 lng/lat）
     * @return 约球数据列表，最多 pageSize+1 条，供 app 层判断是否还有下一页
     */
    public List<MeetupData> listByDistance(MeetupListCmd query) {
        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);

        // 1. 一次 Redis 查询：根据 radiusKm 决定带半径还是全城搜索
        List<NearbyResult> nearbyResults;
        if (query.getRadiusKm() != null) {
            double radiusMeters = query.getRadiusKm().multiply(new BigDecimal("1000")).doubleValue();
            nearbyResults = nearbyRepository.searchByRadius(query.getCityCode(), query.getLng(), query.getLat(), radiusMeters);
        } else {
            nearbyResults = nearbyRepository.searchAllByDistance(query.getCityCode(), query.getLng(), query.getLat());
        }
        if (nearbyResults.isEmpty()) {
            return List.of();
        }

        // 2. 构建筛选条件（不查 Redis）
        MeetupListQueryParam param = queryPlanner.buildFilterParam(query);
        List<String> nearbyIds = nearbyResults.stream()
                .map(NearbyResult::getMeetupId).collect(Collectors.toList());
        param.setMeetupIds(nearbyIds);

        // 3. 数据库查询（带筛选条件，不分页）
        List<MeetupData> allData = meetupRepository.listByMeetupIdsWithFilter(param);

        // 4. 按 Redis 距离顺序排序，并设置距离
        Map<String, Double> distanceMap = nearbyResults.stream()
                .collect(Collectors.toMap(NearbyResult::getMeetupId, NearbyResult::getDistanceMeters, (a, b) -> a));
        Map<String, MeetupData> dataMap = allData.stream()
                .collect(Collectors.toMap(MeetupData::getBizId, d -> d, (a, b) -> a));
        List<MeetupData> sortedData = nearbyIds.stream()
                .filter(dataMap::containsKey)
                .map(dataMap::get)
                .peek(data -> data.setDistanceMeters(distanceMap.get(data.getBizId())))
                .toList();

        // 5. searchAfter 游标：用上一页最后一条 bizId（app 层已解码）定位，取其后 pageSize+1 条
        return PageDTO.sliceAfter(sortedData, query.getLastBizId(), query.getPageSize() + 1, MeetupData::getBizId);
    }

}
