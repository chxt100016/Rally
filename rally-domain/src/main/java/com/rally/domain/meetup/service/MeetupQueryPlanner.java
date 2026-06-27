package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.MeetupListCmd;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import com.rally.domain.utils.Assert;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 约球查询参数规划器
 * 负责将查询命令转换为数据库查询参数（半径过滤由 SQL ST_Distance_Sphere 函数完成，不依赖 Redis）
 */
@Component
public class MeetupQueryPlanner {

    /**
     * 构建时间排序查询参数（listByTime 用）
     * 有 radiusKm 时填充经纬度与半径（米），由 SQL 距离函数做范围过滤
     * @param query 查询命令
     * @return 查询参数
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
                .pageSize(query.getPageSize())
                // 游标已在 app 层解码（时间排序用 {startTime, bizId} 复合游标）
                .lastBizId(query.getLastBizId())
                .lastStartTime(query.getLastStartTime());
        fillRange(builder, query, query.getRadiusKm() != null);
        return builder.build();
    }

    /**
     * 构建距离排序查询参数（不查 Redis，距离由 SQL 函数计算，listByDistance 用）
     * @param query 查询命令（需含 lng/lat）
     * @return 查询参数
     */
    public MeetupListQueryParam buildDistanceParam(MeetupListCmd query) {
        MeetupListQueryParam.MeetupListQueryParamBuilder builder = MeetupListQueryParam.builder()
                .cityCode(query.getCityCode())
                .matchType(query.getMatchType())
                .startTimeFrom(query.getStartTime())
                .startTimeTo(query.getEndTime())
                .levelMode(query.getLevelMode())
                .levelMin(query.getLevelMin())
                .levelMax(query.getLevelMax());
        fillRange(builder, query, true);
        return builder.build();
    }

    /**
     * 填充经纬度查询点；needRadius 为 true 且有 radiusKm 时换算半径（米）。校验 lng/lat 必传
     */
    private void fillRange(MeetupListQueryParam.MeetupListQueryParamBuilder builder, MeetupListCmd query, boolean needRadius) {
        if (!needRadius) {
            return;
        }
        Assert.notNull(query.getLng(), BizErrorCode.PARAM_ERROR);
        Assert.notNull(query.getLat(), BizErrorCode.PARAM_ERROR);
        builder.lng(query.getLng()).lat(query.getLat());
        if (query.getRadiusKm() != null) {
            builder.radiusMeters(query.getRadiusKm().multiply(new BigDecimal("1000")).doubleValue());
        }
    }
}
