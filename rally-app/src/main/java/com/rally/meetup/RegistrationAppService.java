package com.rally.meetup;

import com.rally.domain.meetup.model.*;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.meetup.activity.ApprovePendingRegistrationActivity;
import com.rally.meetup.activity.ApprovedRegistrationContext;
import com.rally.meetup.activity.DispatchMeetupRegistrationNotificationActivity;
import com.rally.meetup.activity.DispatchMemberQuitNotificationActivity;
import com.rally.meetup.activity.DispatchRegistrationApprovedNotificationActivity;
import com.rally.meetup.activity.DispatchTeamSuccessNotificationActivity;
import com.rally.meetup.activity.InvitedParticipantContext;
import com.rally.meetup.activity.JoinApprovedParticipantChatActivity;
import com.rally.meetup.activity.JoinDirectParticipantChatActivity;
import com.rally.meetup.activity.JoinInvitedParticipantChatActivity;
import com.rally.meetup.activity.LeaveMeetupChatActivity;
import com.rally.meetup.activity.LeaveMeetupChatContext;
import com.rally.meetup.activity.MeetupParticipantRegistrationContext;
import com.rally.meetup.activity.MeetupParticipantQuitContext;
import com.rally.meetup.activity.MemberQuitNotificationContext;
import com.rally.meetup.activity.QuitMeetupParticipantActivity;
import com.rally.meetup.activity.RejectPendingRegistrationActivity;
import com.rally.meetup.activity.RegisterInvitedParticipantActivity;
import com.rally.meetup.activity.RegisterMeetupParticipantActivity;
import com.rally.utils.UserContext;
import com.rally.meetup.activity.WithdrawPendingRegistrationActivity;

import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报名/注册服务：报名、撤回、退出、审批通过/拒绝
 * 负责流程编排，领域校验与持久化委托给对应业务活动。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationAppService {

    private final WithdrawPendingRegistrationActivity withdrawPendingRegistrationActivity;
    private final RegisterInvitedParticipantActivity registerInvitedParticipantActivity;
    private final JoinInvitedParticipantChatActivity joinInvitedParticipantChatActivity;
    private final DispatchTeamSuccessNotificationActivity dispatchTeamSuccessNotificationActivity;
    private final RegisterMeetupParticipantActivity registerMeetupParticipantActivity;
    private final JoinDirectParticipantChatActivity joinDirectParticipantChatActivity;
    private final DispatchMeetupRegistrationNotificationActivity dispatchMeetupRegistrationNotificationActivity;
    private final QuitMeetupParticipantActivity quitMeetupParticipantActivity;
    private final LeaveMeetupChatActivity leaveMeetupChatActivity;
    private final DispatchMemberQuitNotificationActivity dispatchMemberQuitNotificationActivity;
    private final ApprovePendingRegistrationActivity approvePendingRegistrationActivity;
    private final JoinApprovedParticipantChatActivity joinApprovedParticipantChatActivity;
    private final DispatchRegistrationApprovedNotificationActivity dispatchRegistrationApprovedNotificationActivity;
    private final RejectPendingRegistrationActivity rejectPendingRegistrationActivity;

    /**
     * 报名
     */
    @Transactional
    public void join(MeetupJoinCmd cmd) {
        String userId = UserContext.get();
        String shareUserId = cmd.getShareUserId();

        if (shareUserId != null) {
            log.info("Joining meetup with shared, userId:{}, shareUserId={}",  userId, shareUserId);

        }

        // 1-2. 校验用户资料和约球准入，建立报名并整体保存聚合。
        MeetupParticipantRegistrationContext registrationContext = registerMeetupParticipantActivity.execute(
                cmd.getMeetupId(), userId, cmd.getAutoWithdrawAt());

        // 直接加入的报名人在同一事务内建立群聊成员，待审批报名跳过。
        joinDirectParticipantChatActivity.execute(
                registrationContext.meetupId(), registrationContext.userId(), registrationContext.status());

        // 3. 在外层事务提交后异步尝试发送本次报名唯一对应的通知场景。
        dispatchMeetupRegistrationNotificationActivity.execute(registrationContext);
    }

    /**
     * 撤回（仅 pending 可撤）
     */
    @Transactional
    public void withdraw(String meetupId) {
        String userId = UserContext.get();

        // 1. 直接查询本人 PENDING/JOINED 报名，只撤回 PENDING；不加载约球聚合。
        withdrawPendingRegistrationActivity.execute(meetupId, userId);

        // 2. 日志
        log.info("撤回报名: userId={}, meetupId={}", userId, meetupId);
    }

    /**
     * 退出（已加入）
     */
    @Transactional
    public void quit(String meetupId) {
        String userId = UserContext.get();

        // 1-2. 加载聚合、退出有效报名、判断处罚并整体保存。
        MeetupParticipantQuitContext quitContext = quitMeetupParticipantActivity.execute(meetupId, userId);

        // 退出群聊并取得通知所需的当前昵称；资料缺失时同一事务回滚。
        LeaveMeetupChatContext chatContext = leaveMeetupChatActivity.execute(
                quitContext.meetupId(), quitContext.userId());

        // 3. 扣分
        // TODO: 调用评分域扣分

        // 4. 安排事务提交后异步通知；通知失败不改变已经完成的退出结果。
        dispatchMemberQuitNotificationActivity.execute(new MemberQuitNotificationContext(
                quitContext.registrationId(),
                quitContext.meetupId(),
                quitContext.creatorId(),
                quitContext.meetupData().getTitle(),
                quitContext.meetupData().getStartTime(),
                chatContext.quitNickname(),
                LocalDateTime.now()));

        // 5. 日志
        log.info("退出成功: userId={}, meetupId={}", userId, meetupId);
    }

    /**
     * 审批通过
     */
    @Transactional
    public void approve(RegistrationApproveCmd cmd) {
        String currentUserId = UserContext.get();

        // 1-2. 加载完整聚合，批准 PENDING 报名并整体保存、重算当前人数。
        ApprovedRegistrationContext approvalContext = approvePendingRegistrationActivity.execute(
                cmd.getMeetupId(), cmd.getRegistrationId(), currentUserId);

        // 在同一事务中建立获批申请人的群聊成员关系，失败回滚审批。
        joinApprovedParticipantChatActivity.execute(
                approvalContext.meetupId(), approvalContext.approvedUserId());

        // 3. 按审批后是否满员选择唯一场景，并在当前事务提交后异步触达。
        dispatchRegistrationApprovedNotificationActivity.execute(approvalContext);
        log.info("审批通过: registrationId={}", cmd.getRegistrationId());
    }

    /**
     * 审批拒绝（仅创建人）
     */
    @Transactional
    public void reject(RegistrationRejectCmd cmd) {
        String currentUserId = UserContext.get();

        // 1-3. 加载聚合，校验报名归属、创建者与 PENDING 状态，并整体保存拒绝结果。
        rejectPendingRegistrationActivity.execute(
                cmd.getMeetupId(), cmd.getRegistrationId(), currentUserId);

        log.info("审批拒绝: registrationId={}", cmd.getRegistrationId());
    }

    /**
     * 邀请用户加入（仅创建人）
     */
    @Transactional
    public void invite(MeetupInviteCmd cmd) {
        String currentUserId = UserContext.get();
        String inviteeUserId = cmd.getUserId();

        // 1-2. 加载聚合、校验邀请并整体保存报名与当前人数。
        InvitedParticipantContext inviteContext = registerInvitedParticipantActivity.execute(
                cmd.getMeetupId(), currentUserId, inviteeUserId);

        // 3. 加入群聊
        joinInvitedParticipantChatActivity.execute(inviteContext.meetupId(), inviteContext.inviteeUserId());

        // 4. 满员时安排提交后异步的组团成功通知；通知失败不改变邀请结果。
        dispatchTeamSuccessNotificationActivity.execute(inviteContext);

        log.info("邀请成功: meetupId={}, inviteeUserId={}, inviterUserId={}", cmd.getMeetupId(), inviteeUserId, currentUserId);
    }
}
