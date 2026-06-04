package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.meetup.entity.WaitlistPO;
import com.rally.db.meetup.service.WaitlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名等待表 Repository（门面层）
 */
@Repository
@RequiredArgsConstructor
public class WaitlistRepository {

    private final WaitlistService waitlistService;

    public WaitlistPO findByBizId(String bizId) {
        return waitlistService.lambdaQuery()
                .eq(WaitlistPO::getBizId, bizId)
                .one();
    }

    public WaitlistPO findActiveByMeetupAndUser(String meetupId, String userId) {
        return waitlistService.lambdaQuery()
                .eq(WaitlistPO::getRallyMeetupId, meetupId)
                .eq(WaitlistPO::getUserId, userId)
                .in(WaitlistPO::getStatus, "pending", "approved")
                .one();
    }

    public WaitlistPO findByMeetupAndUserAny(String meetupId, String userId) {
        return waitlistService.lambdaQuery()
                .eq(WaitlistPO::getRallyMeetupId, meetupId)
                .eq(WaitlistPO::getUserId, userId)
                .orderByDesc(WaitlistPO::getUpdateTime)
                .last("LIMIT 1")
                .one();
    }

    public List<WaitlistPO> findByUserAndStatus(String userId, String status) {
        return waitlistService.lambdaQuery()
                .eq(WaitlistPO::getUserId, userId)
                .eq(WaitlistPO::getStatus, status)
                .list();
    }

    public List<WaitlistPO> findPendingByMeetupId(String meetupId) {
        return waitlistService.lambdaQuery()
                .eq(WaitlistPO::getRallyMeetupId, meetupId)
                .eq(WaitlistPO::getStatus, "pending")
                .orderByDesc(WaitlistPO::getCreateTime)
                .list();
    }

    /**
     * 查询用户在指定时间段内的有效报名（冲突检测用）
     */
    public List<WaitlistPO> findConflict(String userId, LocalDateTime startTime,
                                          LocalDateTime endTime, String excludeMeetupId) {
        // 这里需要关联 rally_meetup 表查询时间冲突
        // MVP 简化：先查用户的有效报名，再在应用层判断时间冲突
        LambdaQueryWrapper<WaitlistPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaitlistPO::getUserId, userId)
                .in(WaitlistPO::getStatus, "pending", "approved");
        if (excludeMeetupId != null) {
            wrapper.ne(WaitlistPO::getRallyMeetupId, excludeMeetupId);
        }
        return waitlistService.list(wrapper);
    }

    public void save(WaitlistPO po) {
        waitlistService.save(po);
    }

    public void updateById(WaitlistPO po) {
        waitlistService.updateById(po);
    }

    /**
     * 复活已失效的报名记录
     */
    public boolean revive(String bizId, LocalDateTime expiresAt) {
        return waitlistService.lambdaUpdate()
                .eq(WaitlistPO::getBizId, bizId)
                .in(WaitlistPO::getStatus, "rejected", "withdrawn", "expired")
                .set(WaitlistPO::getStatus, "pending")
                .set(WaitlistPO::getExpiresAt, expiresAt)
                .set(WaitlistPO::getCreateTime, LocalDateTime.now())
                .update();
    }

    /**
     * 查询约球已批准的参与者 userId 列表
     */
    public List<String> listApprovedUserIds(String meetupId) {
        return waitlistService.lambdaQuery()
                .select(WaitlistPO::getUserId)
                .eq(WaitlistPO::getRallyMeetupId, meetupId)
                .eq(WaitlistPO::getStatus, "approved")
                .list()
                .stream()
                .map(WaitlistPO::getUserId)
                .toList();
    }
}
