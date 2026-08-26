package com.rally.meetup;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.notify.MeetupNotifyAssembler;
import com.rally.notify.NotificationEventId;
import com.rally.utils.UserContext;

import java.util.List;
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
    private final NotificationDeliveryService notificationDeliveryService;

    /**
     * 报名
     */
    @Transactional
    public void join(MeetupJoinCmd cmd) {
        String userId = UserContext.get();
        String shareUserId = cmd.getShareUserId();

        UserProfile userProfile = userProfileDomainService.get(userId);
        userProfile.assertCompleted();

        if (shareUserId != null) {
            log.info("Joining meetup with shared, userId:{}, shareUserId={}",  userId, shareUserId);

        }


        // 1. 查询领域对象
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());

        // 2. 报名（聚合根校验 + 创建报名记录 + 持久化）
        RegistrationStatusEnum status = registrationDomainService.join(meetup, userProfile, cmd.getAutoWithdrawAt());

        // 加入群聊
        if (RegistrationStatusEnum.JOINED == status) {
            this.chatDomainService.join(cmd.getMeetupId(), userId);
        }

        String meetupId = cmd.getMeetupId();
        boolean teamFormed = status == RegistrationStatusEnum.JOINED && meetup.isFull();
        RegistrationData registration = meetup.findActiveRegistration(userId);

        // 3. 发送通知（app 层负责）
        if (status == RegistrationStatusEnum.JOINED) {
            if (teamFormed) {
                // 直接组团成功：只发组团成功给全体参与人（含创建人），不再发报名成功
                notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.TEAM_SUCCESS, registration.getBizId()),
                        NotifyBizType.MEETUP, meetupId, NoticeScene.TEAM_SUCCESS,
                        meetup.getActiveParticipantIds(null), MeetupNotifyAssembler.teamSuccessData(meetup.getData()),
                        uid -> meetupDomainService.shouldNotice(meetupId, uid));
            } else {
                // 免审批未满员：发报名成功通知
                notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.JOIN_SUCCESS, registration.getBizId()),
                        NotifyBizType.MEETUP, meetupId, NoticeScene.JOIN_SUCCESS, List.of(userId),
                        MeetupNotifyAssembler.joinSuccessData(meetup.getData()),
                        uid -> meetupDomainService.shouldNotice(meetupId, uid));
            }
        } else {
            // 需审批：提醒创建人有新申请待审批（申请方=申请人昵称）
            notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.PENDING_APPROVAL, registration.getBizId()),
                    NotifyBizType.MEETUP, meetupId, NoticeScene.PENDING_APPROVAL, List.of(meetup.getCreatorId()),
                    MeetupNotifyAssembler.pendingApprovalData(meetup.getData(), userProfile.getUser().getNickname()));
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
        RegistrationData quittingRegistration = meetup.findActiveRegistration(userId);
        QuitResult result = registrationDomainService.quit(meetup, userId);

        // 退出群聊
        this.chatDomainService.quit(meetupId, userId);

        // 3. 扣分
        // TODO: 调用评分域扣分

        // 4. 通知：通知创建人有成员退出
        UserProfile quitUserProfile = userProfileDomainService.get(userId);
        notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.MEMBER_QUIT, quittingRegistration.getBizId()),
                NotifyBizType.MEETUP, meetupId, NoticeScene.MEMBER_QUIT, List.of(meetup.getCreatorId()),
                MeetupNotifyAssembler.memberQuitData(meetup.getData(), quitUserProfile.getUser().getNickname()));

        // 5. 日志
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

        // 3. 发送通知：审批通过后通知申请人。若本次审批直接组团成功，只发组团成功、不再发报名成功
        String meetupId = cmd.getMeetupId();
        if (meetup.isFull()) {
            notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.TEAM_SUCCESS, cmd.getRegistrationId()),
                    NotifyBizType.MEETUP, meetupId, NoticeScene.TEAM_SUCCESS,
                    meetup.getActiveParticipantIds(null), MeetupNotifyAssembler.teamSuccessData(meetup.getData()),
                    uid -> meetupDomainService.shouldNotice(meetupId, uid));
        } else {
            notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.JOIN_SUCCESS, cmd.getRegistrationId()),
                    NotifyBizType.MEETUP, meetupId, NoticeScene.JOIN_SUCCESS, List.of(userId),
                    MeetupNotifyAssembler.joinSuccessData(meetup.getData()),
                    uid -> meetupDomainService.shouldNotice(meetupId, uid));
        }
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

        log.info("审批拒绝: registrationId={}", cmd.getRegistrationId());
    }

    /**
     * 邀请用户加入（仅创建人）
     */
    @Transactional
    public void invite(MeetupInviteCmd cmd) {
        String currentUserId = UserContext.get();
        String inviteeUserId = cmd.getUserId();

        // 1. 加载约球聚合根
        Meetup meetup = meetupDomainService.get(cmd.getMeetupId());

        // 2. 邀请加入（聚合根校验 + 创建报名记录 + 持久化）
        registrationDomainService.invite(meetup, inviteeUserId, currentUserId);
        RegistrationData invitedRegistration = meetup.findActiveRegistration(inviteeUserId);

        // 3. 加入群聊
        chatDomainService.join(cmd.getMeetupId(), inviteeUserId);

        // 4. 发送通知：邀请成功通知被邀请人。若本次邀请直接组团成功，只发组团成功、不再发报名成功
        String meetupId = cmd.getMeetupId();
        if (meetup.isFull()) {
            notificationDeliveryService.notify(NotificationEventId.of(NoticeScene.TEAM_SUCCESS, invitedRegistration.getBizId()),
                    NotifyBizType.MEETUP, meetupId, NoticeScene.TEAM_SUCCESS,
                    meetup.getActiveParticipantIds(null), MeetupNotifyAssembler.teamSuccessData(meetup.getData()),
                    uid -> meetupDomainService.shouldNotice(meetupId, uid));
        }

        log.info("邀请成功: meetupId={}, inviteeUserId={}, inviterUserId={}", cmd.getMeetupId(), inviteeUserId, currentUserId);
    }
}
