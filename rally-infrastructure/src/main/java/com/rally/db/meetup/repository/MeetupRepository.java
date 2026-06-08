package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.service.MeetupService;
import com.rally.domain.meetup.model.MeetupListQueryParam;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 约球主表 Repository（门面层）
 */
@Repository
@RequiredArgsConstructor
public class MeetupRepository {

    private final MeetupService meetupService;

    public MeetupPO findByBizId(String bizId) {
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getBizId, bizId)
                .one();
    }

    public List<MeetupPO> findByBizIds(List<String> bizIds) {
        if (bizIds == null || bizIds.isEmpty()) {
            return List.of();
        }
        return meetupService.lambdaQuery()
                .in(MeetupPO::getBizId, bizIds)
                .list();
    }

    public List<MeetupPO> findByCityCodeAndStatus(String cityCode, List<String> statusList) {
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCityCode, cityCode)
                .in(MeetupPO::getStatus, statusList)
                .list();
    }

    public void save(MeetupPO po) {
        meetupService.save(po);
    }

    public void updateById(MeetupPO po) {
        meetupService.updateById(po);
    }

    /**
     * 更新状态
     */
    public boolean updateStatus(String bizId, String fromStatus, String toStatus) {
        return meetupService.lambdaUpdate()
                .eq(MeetupPO::getBizId, bizId)
                .eq(MeetupPO::getStatus, fromStatus)
                .set(MeetupPO::getStatus, toStatus)
                .update();
    }

    /**
     * 原子自增人数（报名/审批通过）
     * @return 影响行数，0 表示已满或状态变化
     */
    public int incrementPlayers(String bizId) {
        return meetupService.getBaseMapper().update(null,
                new LambdaUpdateWrapper<MeetupPO>()
                        .eq(MeetupPO::getBizId, bizId)
                        .eq(MeetupPO::getStatus, "OPEN")
                        .apply("current_players < max_players")
                        .setSql("current_players = current_players + 1")
                        .setSql("status = IF(current_players + 1 >= max_players, 'FULL', status)"));
    }

    /**
     * 原子自减人数（退出/拒绝）
     * @return 影响行数
     */
    public int decrementPlayers(String bizId) {
        return meetupService.getBaseMapper().update(null,
                new LambdaUpdateWrapper<MeetupPO>()
                        .eq(MeetupPO::getBizId, bizId)
                        .apply("current_players > 1")
                        .setSql("current_players = current_players - 1")
                        .setSql("status = IF(status = 'FULL', 'OPEN', status)"));
    }

    /**
     * 统计用户当日活跃发布数（status IN OPEN,FULL）
     */
    public long countTodayActive(String userId) {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCreatorId, userId)
                .in(MeetupPO::getStatus, "OPEN", "full")
                .ge(MeetupPO::getCreateTime, todayStart)
                .count();
    }

    /**
     * 查询城市下活跃约球 ID 列表
     */
    public List<String> listActiveIds(String cityCode) {
        return meetupService.lambdaQuery()
                .select(MeetupPO::getBizId)
                .eq(MeetupPO::getCityCode, cityCode)
                .in(MeetupPO::getStatus, "OPEN", "full")
                .gt(MeetupPO::getEndTime, LocalDateTime.now())
                .list()
                .stream()
                .map(MeetupPO::getBizId)
                .toList();
    }

    /**
     * 批量更新状态为 FINISHED（兜底任务用）
     * @return 影响行数
     */
    public int batchUpdateToFinished() {
        return meetupService.getBaseMapper().update(null,
                new LambdaUpdateWrapper<MeetupPO>()
                        .in(MeetupPO::getStatus, "OPEN", "full")
                        .lt(MeetupPO::getEndTime, LocalDateTime.now())
                        .set(MeetupPO::getStatus, "FINISHED"));
    }

    /**
     * 统计用户近 N 天内完成的约球场数（可信度计算用）
     * 条件：status=finished 且 end_time 在近 N 天内，用户为发布者或已批准报名者
     */
    public long countFinishedMatches(String userId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        // 统计用户作为发布者的已完成约球
        return meetupService.lambdaQuery()
                .eq(MeetupPO::getCreatorId, userId)
                .eq(MeetupPO::getStatus, "FINISHED")
                .ge(MeetupPO::getEndTime, since)
                .count();
        // TODO: 还需要统计用户作为报名者（已批准）的已完成约球，需要关联 waitlist 表
    }

    /**
     * 查询可报名的约球列表（带筛选、排序、分页）
     */
    public IPage<MeetupPO> listNew(MeetupListQueryParam param) {
        LambdaQueryWrapper<MeetupPO> wrapper = new LambdaQueryWrapper<>();

        // 基础条件：城市 + 状态 + 未结束
        wrapper.eq(MeetupPO::getCityCode, param.getCityCode())
                .in(MeetupPO::getStatus, "OPEN", "FULL")
                .gt(MeetupPO::getEndTime, LocalDateTime.now());

        // 约球ID列表筛选（距离查询时使用）
        if (!CollectionUtils.isEmpty(param.getMeetupIds())) {
            wrapper.in(MeetupPO::getBizId, param.getMeetupIds());
        }

        // 类型筛选
        if (param.getMatchType() != null) {
            wrapper.eq(MeetupPO::getMatchType, param.getMatchType().name());
        }

        // 时间范围筛选
        if (param.getStartTimeFrom() != null) {
            wrapper.ge(MeetupPO::getStartTime, param.getStartTimeFrom());
        }
        if (param.getStartTimeTo() != null) {
            wrapper.le(MeetupPO::getStartTime, param.getStartTimeTo());
        }

        // 水平筛选（判断查询范围与约球水平是否有交集）
        if (param.getLevelMin() != null && param.getLevelMax() != null) {
            String queryMin = param.getLevelMin().toPlainString();
            String queryMax = param.getLevelMax().toPlainString();
            wrapper.and(w -> w
                    // RANGE 模式：levelValue 格式为 "min:max"，判断区间是否有交集
                    .and(inner -> inner
                            .eq(MeetupPO::getLevelMode, "RANGE")
                            .apply("SUBSTRING_INDEX(level_value, ':', 1) <= {0}", queryMax)
                            .apply("SUBSTRING_INDEX(level_value, ':', -1) >= {0}", queryMin)
                    )
                    // EXACT 模式：levelValue 为精确值，判断是否在查询范围内
                    .or(inner -> inner
                            .eq(MeetupPO::getLevelMode, "EXACT")
                            .ge(MeetupPO::getLevelValue, queryMin)
                            .le(MeetupPO::getLevelValue, queryMax)
                    )
                    // ABOVE 模式：levelValue 为下限，判断下限 <= queryMax
                    .or(inner -> inner
                            .eq(MeetupPO::getLevelMode, "ABOVE")
                            .le(MeetupPO::getLevelValue, queryMax)
                    )
                    // BELOW 模式：levelValue 为上限，判断上限 >= queryMin
                    .or(inner -> inner
                            .eq(MeetupPO::getLevelMode, "BELOW")
                            .ge(MeetupPO::getLevelValue, queryMin)
                    )
            );
        }

        // 排序
        wrapper.orderByDesc(MeetupPO::getCreateTime);

        // 分页
        Page<MeetupPO> page = new Page<>(param.getPageNo(), param.getPageSize());
        return meetupService.page(page, wrapper);
    }

    /**
     * 按 meetupId 列表 + 筛选条件查询（不分页，距离排序用）
     */
    public List<MeetupPO> listByMeetupIdsWithFilter(MeetupListQueryParam param) {
        LambdaQueryWrapper<MeetupPO> wrapper = new LambdaQueryWrapper<>();

        // 基础条件：城市 + 状态 + 未结束
        wrapper.eq(MeetupPO::getCityCode, param.getCityCode())
                .in(MeetupPO::getStatus, "OPEN", "FULL")
                .gt(MeetupPO::getEndTime, LocalDateTime.now());

        // 约球ID列表筛选
        if (!CollectionUtils.isEmpty(param.getMeetupIds())) {
            wrapper.in(MeetupPO::getBizId, param.getMeetupIds());
        }

        // 类型筛选
        if (param.getMatchType() != null) {
            wrapper.eq(MeetupPO::getMatchType, param.getMatchType().name());
        }

        // 时间范围筛选
        if (param.getStartTimeFrom() != null) {
            wrapper.ge(MeetupPO::getStartTime, param.getStartTimeFrom());
        }
        if (param.getStartTimeTo() != null) {
            wrapper.le(MeetupPO::getStartTime, param.getStartTimeTo());
        }

        // 水平筛选
        if (param.getLevelMin() != null && param.getLevelMax() != null) {
            String queryMin = param.getLevelMin().toPlainString();
            String queryMax = param.getLevelMax().toPlainString();
            wrapper.and(w -> w
                    .and(inner -> inner
                            .eq(MeetupPO::getLevelMode, "RANGE")
                            .apply("SUBSTRING_INDEX(level_value, ':', 1) <= {0}", queryMax)
                            .apply("SUBSTRING_INDEX(level_value, ':', -1) >= {0}", queryMin)
                    )
                    .or(inner -> inner
                            .eq(MeetupPO::getLevelMode, "EXACT")
                            .ge(MeetupPO::getLevelValue, queryMin)
                            .le(MeetupPO::getLevelValue, queryMax)
                    )
                    .or(inner -> inner
                            .eq(MeetupPO::getLevelMode, "ABOVE")
                            .le(MeetupPO::getLevelValue, queryMax)
                    )
                    .or(inner -> inner
                            .eq(MeetupPO::getLevelMode, "BELOW")
                            .ge(MeetupPO::getLevelValue, queryMin)
                    )
            );
        }

        // 不分页，返回所有结果
        return meetupService.list(wrapper);
    }
}
