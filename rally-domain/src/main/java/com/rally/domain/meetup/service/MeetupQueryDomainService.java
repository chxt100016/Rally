package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.model.*;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 约球查询领域服务
 * 负责广场列表查询（按时间/距离）等读操作的核心逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MeetupQueryDomainService {

    private final MeetupRepository meetupRepository;
    private final MeetupQueryPlanner queryPlanner;

    /**
     * 按时间排序的列表查询（searchAfter 游标分页）
     * @param query 查询条件
     * @return 约球数据列表，最多 pageSize+1 条，供 app 层判断是否还有下一页
     */
    public List<MeetupData> listByTime(MeetupListCmd query) {
        MeetupListQueryParam param = queryPlanner.plan(query);
        return meetupRepository.listAvailable(param);
    }

    /**
     * 按距离排序的列表查询（searchAfter 游标分页）
     * 流程：一次 SQL 查询（ST_Distance_Sphere 函数算距离 + 范围过滤 + 距离升序）→ 按游标取窗口
     * 数据量不大，距离计算与排序下推数据库，不依赖 Redis GEO
     * @param query 查询条件（必须包含 lng/lat）
     * @return 约球数据列表，最多 pageSize+1 条，供 app 层判断是否还有下一页
     */
    public List<MeetupData> listByDistance(MeetupListCmd query) {
        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);

        // 1. SQL 查询：城市内符合筛选条件、范围内的约球，已按距离升序排好，距离（米）写入 distanceMeters
        MeetupListQueryParam param = queryPlanner.buildDistanceParam(query);
        List<MeetupData> sortedData = meetupRepository.listByDistance(param);

        // 2. searchAfter 游标：用上一页最后一条 bizId（app 层已解码）定位，取其后 pageSize+1 条
        return PageDTO.sliceAfter(sortedData, query.getLastBizId(), query.getPageSize() + 1, MeetupData::getBizId);
    }

}
