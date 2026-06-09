package com.rally.meetup;

import com.rally.utils.UserContext;
import com.rally.domain.meetup.model.JoinResult;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.QuitResult;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 报名/注册服务：报名、撤回、退出、审批通过/拒绝
 * 负责流程编排，领域校验与持久化委托给 {@link RegistrationDomainService}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationAppService {

    private final MeetupDomainService meetupDomainService;
    private final RegistrationDomainService registrationDomainService;
    private final UserProfileDomainService userProfileDomainService;

    /**
     * 报名
     */
    @Transactional
    public void join(String meetupId, LocalDateTime autoWithdrawAt) {
        String userId = UserContext.get();

        // 1. 查询领域对象
        Meetup meetup = meetupDomainService.get(meetupId);
        UserProfile userProfile = userProfileDomainService.get(userId);

        // 2. 报名（聚合根校验 + 创建报名记录 + 持久化）
        RegistrationData registration = registrationDomainService.join(meetup, userProfile, autoWithdrawAt);

        // 3. 根据报名记录状态判断结果
        JoinResult result = JoinResult.fromRegistration(registration);

        // 4. 发送通知（app 层负责）
        if (result == JoinResult.APPROVED) {
            // TODO: 调用通知域
            log.info("报名通过: userId={}, meetupId={}", userId, meetupId);
        } else {
            // TODO: 调用通知域
            log.info("新申请: userId={}, meetupId={}", userId, meetupId);
        }
    }

    /**
     * 撤回（仅 pending 可撤）
     */
    @Transactional
    public void withdraw(String meetupId) {
        String userId = UserContext.get();

        // 1. 领域校验 + 持久化
        registrationDomainService.withdraw(meetupId, userId);

        // 2. 日志
        log.info("撤回报名: userId={}, meetupId={}", userId, meetupId);
    }

    /**
     * 退出（已加入）
     */
    @Transactional
    public void quit(String meetupId) {
        String userId = UserContext.get();

        // 1. 查询约球聚合根（含报名记录）
        Meetup meetup = meetupDomainService.get(meetupId);

        // 2. 退出（聚合根校验 + 持久化），返回是否需扣分
        QuitResult result = registrationDomainService.quit(meetup, userId);

        // 3. 扣分
        if (result == QuitResult.PENALIZED) {
            int penalty = SystemConfig.getInt("meetup.quit.penalty_under_6h", 25);
            // TODO: 调用评分域扣分
            log.info("退出扣分: userId={}, meetupId={}, penalty={}", userId, meetupId, penalty);
        }

        // 4. 日志
        log.info("退出成功: userId={}, meetupId={}", userId, meetupId);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(String registrationId) {
        String currentUserId = UserContext.get();

        // 1. 获取约球 ID 并加载聚合根
        String meetupId = registrationDomainService.getMeetupIdByRegistration(registrationId);
        Meetup meetup = meetupDomainService.get(meetupId);

        // 2. 审批通过（聚合根校验 + 持久化）
        registrationDomainService.approve(meetup, registrationId, currentUserId);

        // 3. 发送通知
        // TODO: 调用通知域
        log.info("审批通过: registrationId={}", registrationId);
    }

    /**
     * 审批拒绝（仅创建人）
     */
    @Transactional
    public void reject(String registrationId) {
        String currentUserId = UserContext.get();

        // 1. 获取约球 ID 并加载聚合根
        String meetupId = registrationDomainService.getMeetupIdByRegistration(registrationId);
        Meetup meetup = meetupDomainService.get(meetupId);

        // 2. 审批拒绝（聚合根校验 + 持久化）
        registrationDomainService.reject(meetup, registrationId, currentUserId);

        // 3. 发送通知
        // TODO: 调用通知域
        log.info("审批拒绝: registrationId={}", registrationId);
    }
}
