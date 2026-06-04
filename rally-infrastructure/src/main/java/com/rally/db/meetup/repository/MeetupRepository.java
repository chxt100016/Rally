package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.rally.db.meetup.entity.MeetupPO;
import com.rally.db.meetup.service.MeetupService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
}
