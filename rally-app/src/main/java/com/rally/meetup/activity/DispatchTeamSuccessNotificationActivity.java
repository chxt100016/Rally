package com.rally.meetup.activity;

import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.notify.MeetupNotifyAssembler;
import com.rally.notify.NotificationEventId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 dispatch-team-success-notification：邀请满员后尽力通知有效参与者。
 */
@Component
@RequiredArgsConstructor
public class DispatchTeamSuccessNotificationActivity {

    private final MeetupDomainService meetupDomainService;

    private final NotificationDeliveryService notificationDeliveryService;

    public void execute(InvitedParticipantContext context) {
        // A1：邀请后仍未满员时正常结束，不发送单独的邀请成功通知。
        if (!context.full()) {
            return;
        }

        String meetupId = context.meetupId();

        // A2-A5：触达命令在邀请事务提交后异步执行，按候选去重并逐人复核资格；
        // 报名编号形成稳定事件，首次取得微信渠道执行权的任务才会实际发送。
        notificationDeliveryService.notify(
                NotificationEventId.of(NoticeScene.TEAM_SUCCESS, context.registrationId()),
                NotifyBizType.MEETUP,
                meetupId,
                NoticeScene.TEAM_SUCCESS,
                context.participantUserIds(),
                MeetupNotifyAssembler.teamSuccessData(context.meetupData()),
                userId -> meetupDomainService.shouldNotice(meetupId, userId));
    }
}
