package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.enums.UserMeetupTabEnum;
import com.rally.domain.meetup.gateway.MeetupRepository;
import com.rally.domain.meetup.gateway.NearbyRepository;
import com.rally.domain.meetup.gateway.RegistrationRepository;
import com.rally.domain.meetup.model.*;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
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

        // 5. searchAfter 游标：解码出 bizId，定位上一页最后一条记录，取其后 pageSize+1 条
        PageCursor cursor = PageCursor.decode(query.getLastId());
        String lastBizId = cursor != null ? cursor.getBizId() : null;
        int start = 0;
        if (StringUtils.isNotBlank(lastBizId)) {
            for (int i = 0; i < sortedData.size(); i++) {
                if (sortedData.get(i).getBizId().equals(lastBizId)) {
                    start = i + 1;
                    break;
                }
            }
        }
        int end = Math.min(start + query.getPageSize() + 1, sortedData.size());
        return start < sortedData.size() ? sortedData.subList(start, end) : List.of();
    }

    /**
     * 用户约球列表查询（按 Tab 筛选，searchAfter 游标分页）
     * 一个入口，五个分支各自独立
     */
    public PageDTO<MeetupData> listByUser(UserMeetupListCmd cmd, String userId) {
        // searchAfter：多查1条用于判断 hasMore，limit = size + 1
        int limit = cmd.getSize() + 1;
        // 用户 Tab 为单字段游标，解码出 bizId 即可
        PageCursor cursor = PageCursor.decode(cmd.getLastId());
        String lastId = cursor != null ? cursor.getBizId() : null;
        return switch (cmd.getTab()) {
            case PENDING -> listPending(userId, lastId, limit);
            case IN_PROGRESS -> listInProgress(userId, lastId, limit);
            case MY_PUBLISH -> listMyPublish(userId, lastId, limit);
            case COMPLETED -> listCompleted(userId, lastId, limit);
            case RECENT -> listRecent(userId, lastId, limit);
        };
    }

    // ======================== 五个 Tab 分支 ========================

    /**
     * 待处理：创建人有 pending 报名待审批 + 参参与者已结束但未录比分 + 有未读消息
     * UNION SQL searchAfter 游标分页，review deadline 过滤在 SQL 中完成
     */
    private PageDTO<MeetupData> listPending(String userId, String lastId, int limit) {
        int deadlineDays = SystemConfig.getInt(SystemConfigKey.REVIEW_DEADLINE_DAYS.getKey());
        return meetupRepository.listPendingMeetups(userId, deadlineDays, lastId, limit);
    }

    /**
     * 进行中：我创建或我已批准参与 + status IN (OPEN,FULL) + 未到结束时间
     */
    private PageDTO<MeetupData> listInProgress(String userId, String lastId, int limit) {
        MeetupListQueryParam param = MeetupListQueryParam.builder()
                .userId(userId).statusList(List.of("OPEN", "FULL"))
                .registrationStatuses(RegistrationStatusEnum.getParticipated())
                .lastId(lastId).limit(limit).build();
        return doList(param);
    }

    /**
     * 我发布：创建人是当前用户，按创建时间倒序
     */
    private PageDTO<MeetupData> listMyPublish(String userId, String lastId, int limit) {
        MeetupListQueryParam param = MeetupListQueryParam.builder()
                .creatorId(userId)
                .lastId(lastId).limit(limit).build();
        return doList(param);
    }

    /**
     * 已完成：status=FINISHED/CLOSED 或懒判定已结束（OPEN/FULL 且 end_time < now）
     */
    private PageDTO<MeetupData> listCompleted(String userId, String lastId, int limit) {
        MeetupListQueryParam param = MeetupListQueryParam.builder()
                .userId(userId).statusList(List.of("FINISHED"))
                .registrationStatuses(RegistrationStatusEnum.getParticipated())
                .lastId(lastId).limit(limit).build();
        return doList(param);
    }

    /**
     * 最近：用户为创建人或已批准报名的约球，不限状态（球员主页用）
     */
    private PageDTO<MeetupData> listRecent(String userId, String lastId, int limit) {
        return meetupRepository.listRecentByUser(userId, lastId, limit);
    }

    // ======================== 内部工具方法 ========================

    /** 执行用户维度分页查询（listByUser 各分支用） */
    private PageDTO<MeetupData> doList(MeetupListQueryParam param) {
        return meetupRepository.listByUserFilter(param);
    }

}
