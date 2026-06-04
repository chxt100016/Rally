package com.rally.domain.meetup.gateway;

import com.rally.domain.meetup.enums.WaitlistStatusEnum;
import com.rally.domain.meetup.model.WaitlistData;

import java.util.List;

/**
 * 报名等待表读写网关接口
 */
public interface WaitlistGateway {
    /**
     * 保存报名记录（新增或更新）
     */
    void save(WaitlistData data);

    /**
     * 根据 bizId 查询
     */
    WaitlistData findByBizId(String bizId);

    /**
     * 查询用户在某约球的最新有效报名（pending/approved）
     */
    WaitlistData findActiveByMeetupAndUser(String meetupId, String userId);

    /**
     * 查询用户在某约球的任意状态报名记录（用于复报名判断）
     */
    WaitlistData findByMeetupAndUserAny(String meetupId, String userId);

    /**
     * 查询用户的报名状态列表
     */
    List<WaitlistData> findByUserAndStatus(String userId, WaitlistStatusEnum status);

    /**
     * 查询约球的待审批列表
     */
    List<WaitlistData> findPendingByMeetupId(String meetupId);

    /**
     * 查询用户在指定时间段内的有效报名（冲突检测用）
     * @param userId 用户 ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param excludeMeetupId 排除的约球 ID（编辑/重报名时排除自身）
     * @return 冲突的报名列表
     */
    List<WaitlistData> findConflict(String userId, java.time.LocalDateTime startTime,
                                     java.time.LocalDateTime endTime, String excludeMeetupId);

    /**
     * 根据 bizId 更新状态
     */
    void updateStatus(String bizId, WaitlistStatusEnum status);

    /**
     * 复活已失效的报名记录（rejected/withdrawn/expired → pending）
     */
    void revive(String bizId, java.time.LocalDateTime expiresAt);
}
