package com.rally.meetup;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ChatDomainService chatDomainService;

    /**
     * 报名
     */
    @Transactional
    public void join(MeetupJoinCmd cmd) {
        String userId = UserContext.get();
        String shareUserId = cmd.getShareUserId();

        UserProfile userProfile = userProfileDomainService.get(userId);
        // 不是通过分享进入需要校验信息是否完整
        boolean fromShare = false;
        if (shareUserId == null) {
            userProfile.assertCompleted();
        } else {
            fromShare = true;
            log.info("Joining meetup with shared, userId:{}, shareUserId={}",  userId, shareUserId);
        }


        // 1. 查询领域对象
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());




        // 2. 报名（聚合根校验 + 创建报名记录 + 持久化）
        RegistrationStatusEnum status = registrationDomainService.join(meetup, userProfile, cmd.getAutoWithdrawAt(), fromShare);

        // 加入群聊
        if (RegistrationStatusEnum.JOINED == status) {
            this.chatDomainService.join(cmd.getMeetupId(), userId);
        }


        // 3. 发送通知（app 层负责）
        if (status == RegistrationStatusEnum.JOINED) {
            // TODO: 调用通知域
//            log.info("报名通过: userId={}, meetupId={}", userId, meetupId);
        } else {
            // TODO: 调用通知域
//            log.info("新申请: userId={}, meetupId={}", userId, meetupId);
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

        // 退出群聊
        this.chatDomainService.quit(meetupId, userId);

        // 3. 扣分
        // TODO: 调用评分域扣分

        // 4. 通知
        // 通知

        // 4. 日志
        log.info("退出成功: userId={}, meetupId={}", userId, meetupId);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(RegistrationApproveCmd cmd) {
        String currentUserId = UserContext.get();

        // 1. 获取约球 ID 并加载聚合根
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());

        // 2. 审批通过（聚合根校验 + 持久化）
        String userId = registrationDomainService.approve(meetup, cmd.getRegistrationId(), currentUserId);

        // 加入群聊
        this.chatDomainService.join(cmd.getMeetupId(), userId);

        // 3. 发送通知
        // TODO: 调用通知域
        log.info("审批通过: registrationId={}", cmd.getRegistrationId());
    }

    /**
     * 审批拒绝（仅创建人）
     */
    @Transactional
    public void reject(RegistrationRejectCmd cmd) {
        String currentUserId = UserContext.get();

        // 1. 获取约球 ID 并加载聚合根
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());

        // 2. 审批拒绝（聚合根校验 + 持久化）
        registrationDomainService.reject(meetup, cmd.getRegistrationId(), currentUserId);

        // 3. 发送通知
        // TODO: 调用通知域
        log.info("审批拒绝: registrationId={}", cmd.getRegistrationId());
    }
}
