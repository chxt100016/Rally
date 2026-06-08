package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.convert.MeetupDomainConvertMapper;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.NearbyGateway;
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
 * 负责列表查询（按时间/距离）、详情查询等读操作的核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryDomainService {

    private final MeetupGateway meetupGateway;
    private final NearbyGateway nearbyGateway;
    private final MeetupQueryPlanner queryPlanner;

    /**
     * 按时间排序的列表查询
     * @param query 查询条件
     * @return 约球卡片分页结果
     */
    public PageDTO<MeetupCardDTO> listByTime(MeetupListCmd query) {
        MeetupListQueryParam param = queryPlanner.plan(query);
        if (param == null) {
            return MeetupQueryPlanner.emptyPage();
        }
        return doList(param);
    }

    /**
     * 按距离排序的列表查询（GEO 特化）
     * 流程：一次 Redis 查询（距离+范围） → 数据库筛选 → 应用层排序分页
     * @param query 查询条件（必须包含 lng/lat）
     * @return 约球卡片分页结果
     */
    public PageDTO<MeetupCardDTO> listByDistance(MeetupListCmd query) {
        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);

        // 1. 一次 Redis 查询：根据 radiusKm 决定带半径还是全城搜索
        List<NearbyResult> nearbyResults;
        if (query.getRadiusKm() != null) {
            double radiusMeters = query.getRadiusKm().multiply(new BigDecimal("1000")).doubleValue();
            nearbyResults = nearbyGateway.searchByRadius(query.getCityCode(), query.getLng(), query.getLat(), radiusMeters);
        } else {
            nearbyResults = nearbyGateway.searchAllByDistance(query.getCityCode(), query.getLng(), query.getLat());
        }
        if (nearbyResults.isEmpty()) {
            return MeetupQueryPlanner.emptyPage();
        }

        // 2. 构建筛选条件（不查 Redis）
        MeetupListQueryParam param = queryPlanner.buildFilterParam(query);
        List<String> nearbyIds = nearbyResults.stream()
                .map(NearbyResult::getMeetupId).collect(Collectors.toList());
        param.setMeetupIds(nearbyIds);

        // 3. 数据库查询（带筛选条件，不分页）
        List<MeetupData> allData = meetupGateway.listByMeetupIdsWithFilter(param);

        // 4. 按 Redis 距离顺序排序
        Map<String, Double> distanceMap = nearbyResults.stream()
                .collect(Collectors.toMap(NearbyResult::getMeetupId, NearbyResult::getDistanceMeters, (a, b) -> a));
        Map<String, MeetupData> dataMap = allData.stream()
                .collect(Collectors.toMap(MeetupData::getBizId, d -> d, (a, b) -> a));
        List<MeetupData> sortedData = nearbyIds.stream()
                .filter(dataMap::containsKey)
                .map(dataMap::get)
                .collect(Collectors.toList());

        // 5. 内存分页
        int start = (query.getPageNo() - 1) * query.getPageSize();
        int end = Math.min(start + query.getPageSize(), sortedData.size());
        List<MeetupData> pageData = start < sortedData.size() ? sortedData.subList(start, end) : List.of();
        boolean hasMore = end < sortedData.size();

        // 6. 转换 DTO 并设置距离
        List<MeetupCardDTO> cardList = pageData.stream().map(data -> {
            MeetupCardDTO card = MeetupDomainConvertMapper.INSTANCE.toMeetupCardDTO(data);
            card.setDistanceMeters(distanceMap.get(data.getBizId()));
            return card;
        }).collect(Collectors.toList());

        return new PageDTO<>(cardList, (long) sortedData.size(), hasMore);
    }

    /**
     * 执行查询并转换结果
     */
    private PageDTO<MeetupCardDTO> doList(MeetupListQueryParam param) {
        PageDTO<MeetupData> pageResult = meetupGateway.listAvailable(param);

        List<MeetupCardDTO> cardList = pageResult.getList().stream()
                .map(MeetupDomainConvertMapper.INSTANCE::toMeetupCardDTO)
                .collect(Collectors.toList());

        return new PageDTO<>(cardList, pageResult.getTotal(), pageResult.getHasMore());
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
}
