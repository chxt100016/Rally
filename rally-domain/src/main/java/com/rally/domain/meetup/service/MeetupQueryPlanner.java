package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.gateway.NearbyGateway;
import com.rally.domain.meetup.model.MeetupListCmd;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import com.rally.domain.meetup.model.NearbyResult;
import com.rally.domain.meetup.model.PageDTO;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 约球查询参数规划器
 * 负责将查询命令转换为数据库查询参数，处理 Redis GEO 查询等逻辑
 */
@Component
@RequiredArgsConstructor
public class MeetupQueryPlanner {

    private final NearbyGateway nearbyGateway;

    /**
     * 返回空分页结果
     */
    public static <T> PageDTO<T> emptyPage() {
        return new PageDTO<>(List.of(), 0L, false);
    }

    /**
     * 构建查询参数
     * @param query 查询命令
     * @return 查询参数，如果 radiusKm 查询无结果返回 null
     */
    public MeetupListQueryParam plan(MeetupListCmd query) {
        MeetupListQueryParam.MeetupListQueryParamBuilder builder = MeetupListQueryParam.builder()
                .cityCode(query.getCityCode())
                .matchType(query.getMatchType())
                .startTimeFrom(query.getStartTime())
                .startTimeTo(query.getEndTime())
                .levelMode(query.getLevelMode())
                .levelMin(query.getLevelMin())
                .levelMax(query.getLevelMax())
                .pageNo(query.getPageNo())
                .pageSize(query.getPageSize());

        // 如果有 radiusKm，通过 Redis 获取范围内的 meetupId 列表
        List<String> meetupIds = searchByRadius(query);
        if (meetupIds != null && meetupIds.isEmpty()) {
            return null;
        } else if (meetupIds != null) {
            builder.meetupIds(meetupIds);
        }

        return builder.build();
    }

    /**
     * 通过 Redis GEO 查询范围内的 meetupId 列表, 为空就是范围内没有， 为null说明没有限制
     * @return meetupId 列表，如果无结果返回 null
     */
    private List<String> searchByRadius(MeetupListCmd query) {
        if (query.getRadiusKm() == null) {
            return null;
        }

        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);

        double radiusMeters = query.getRadiusKm().multiply(new BigDecimal("1000")).doubleValue();
        List<NearbyResult> nearbyResults = nearbyGateway.searchByRadius(query.getCityCode(), query.getLng(), query.getLat(), radiusMeters);

        return nearbyResults.stream()
                .map(NearbyResult::getMeetupId)
                .collect(Collectors.toList());
    }
}
