package com.rally.db.meetup.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rally.db.meetup.entity.RegistrationPO;
import com.rally.db.meetup.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名/注册表 Repository（门面层）
 */
@Repository
@RequiredArgsConstructor
public class RegistrationRepository {

    private final RegistrationService registrationService;

    public RegistrationPO findByBizId(String bizId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getBizId, bizId)
                .one();
    }

    public RegistrationPO findActiveByMeetupAndUser(String meetupId, String userId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getUserId, userId)
                .in(RegistrationPO::getStatus, "pending", "JOINED")
                .one();
    }

    public RegistrationPO findByMeetupAndUserAny(String meetupId, String userId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getUserId, userId)
                .orderByDesc(RegistrationPO::getUpdateTime)
                .last("LIMIT 1")
                .one();
    }

    public List<RegistrationPO> findByUserAndStatus(String userId, String status) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getUserId, userId)
                .eq(RegistrationPO::getStatus, status)
                .list();
    }

    public List<RegistrationPO> findPendingByMeetupId(String meetupId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getStatus, "pending")
                .orderByDesc(RegistrationPO::getCreateTime)
                .list();
    }

    public List<RegistrationPO> findByMeetupId(String meetupId) {
        return registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .list();
    }

    /**
     * 查询用户在指定时间段内的有效报名（冲突检测用）
     */
    public List<RegistrationPO> findConflict(String userId, LocalDateTime startTime,
                                              LocalDateTime endTime, String excludeMeetupId) {
        LambdaQueryWrapper<RegistrationPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RegistrationPO::getUserId, userId)
                .in(RegistrationPO::getStatus, "pending", "JOINED");
        if (excludeMeetupId != null) {
            wrapper.ne(RegistrationPO::getRallyMeetupId, excludeMeetupId);
        }
        return registrationService.list(wrapper);
    }

    public void save(RegistrationPO po) {
        registrationService.save(po);
    }

    public void updateById(RegistrationPO po) {
        registrationService.updateById(po);
    }

    /**
     * 复活已失效的报名记录
     */
    public boolean revive(String bizId, LocalDateTime expiresAt) {
        return registrationService.lambdaUpdate()
                .eq(RegistrationPO::getBizId, bizId)
                .in(RegistrationPO::getStatus, "REJECTED", "WITHDRAWN", "EXPIRED")
                .set(RegistrationPO::getStatus, "pending")
                .set(RegistrationPO::getExpiresAt, expiresAt)
                .set(RegistrationPO::getCreateTime, LocalDateTime.now())
                .update();
    }

    /**
     * 查询约球已批准的参与者 userId 列表
     */
    public List<String> listApprovedUserIds(String meetupId) {
        return registrationService.lambdaQuery()
                .select(RegistrationPO::getUserId)
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getStatus, "JOINED")
                .list()
                .stream()
                .map(RegistrationPO::getUserId)
                .toList();
    }

    /**
     * 统计约球已批准的参与者数量
     */
    public int countApprovedByMeetupId(String meetupId) {
        Long count = registrationService.lambdaQuery()
                .eq(RegistrationPO::getRallyMeetupId, meetupId)
                .eq(RegistrationPO::getStatus, "JOINED")
                .count();
        return count.intValue();
    }
}
