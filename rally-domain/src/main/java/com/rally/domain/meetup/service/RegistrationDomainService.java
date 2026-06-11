package com.rally.domain.meetup.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.meetup.gateway.RegistrationGateway;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.QuitResult;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 报名领域服务（负责报名的持久化操作）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationDomainService {

    private final MeetupGateway meetupGateway;
    private final RegistrationGateway registrationGateway;

    /**
     * 保存报名（调用聚合根 join + 持久化）
     * @param meetup 约球聚合根
     * @param userProfile 用户档案领域对象
     * @param autoWithdrawAt 自动撤回时间，可为 null
     * @return 报名状态
     */
    public RegistrationStatusEnum join(Meetup meetup, UserProfile userProfile, LocalDateTime autoWithdrawAt) {
        // 1. 调用聚合根 join 方法（校验 + 创建报名记录）
        RegistrationData registration = meetup.join(userProfile, autoWithdrawAt);

        // 2. 只保存新增的报名记录
        registrationGateway.save(registration);

        return registration.getStatus();
    }

    /**
     * 撤回报名（仅 pending 可撤）
     * @param meetupId 约球 ID
     * @param userId 当前用户 ID
     */
    public void withdraw(String meetupId, String userId) {
        // 1. 查询报名记录
        RegistrationData registration = registrationGateway.findActiveByMeetupAndUser(meetupId, userId);
        Assert.notNull(registration, BizErrorCode.NOT_JOINED);

        // 2. 状态校验（委托实体自身行为）
        Assert.isTrue(registration.canWithdraw(), BizErrorCode.WAITLIST_NOT_PENDING);

        // 3. 更新状态
        registrationGateway.updateStatus(registration.getBizId(), RegistrationStatusEnum.WITHDRAWN);
    }

    /**
     * 退出报名（已加入）
     * @param meetup 约球聚合根（含报名记录）
     * @param userId 当前用户 ID
     * @return 退出结果（NORMAL / PENALIZED）
     */
    public QuitResult quit(Meetup meetup, String userId) {
        // 1. 调用聚合根 quit（校验 + 更新状态）
        QuitResult result = meetup.quit(userId);

        // 2. 持久化（自动计算 currentPlayers）
        meetupGateway.save(meetup);

        return result;
    }

    /**
     * 审批通过
     * @param meetup 约球聚合根（含报名记录）
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public void approve(Meetup meetup, String registrationId, String currentUserId) {
        // 1. 调用聚合根 approve（校验 + 更新状态）
        meetup.approve(registrationId, currentUserId);

        // 2. 持久化（自动计算 currentPlayers）
        meetupGateway.save(meetup);
    }

    /**
     * 审批拒绝（仅创建人）
     * @param meetup 约球聚合根（含报名记录）
     * @param registrationId 报名记录 ID
     * @param currentUserId 当前用户 ID（审批人）
     */
    public void reject(Meetup meetup, String registrationId, String currentUserId) {
        // 1. 调用聚合根 reject（校验 + 更新状态）
        meetup.reject(registrationId, currentUserId);

        // 2. 持久化
        meetupGateway.save(meetup);
    }

    /**
     * 根据报名记录 ID 获取约球 ID（供 app 层加载聚合根用）
     * @param registrationId 报名记录 ID
     * @return 约球 ID
     */
    public String getMeetupIdByRegistration(String registrationId) {
        RegistrationData registration = registrationGateway.findByBizId(registrationId);
        Assert.notNull(registration, BizErrorCode.WAITLIST_NOT_FOUND);
        return registration.getRallyMeetupId();
    }
}
