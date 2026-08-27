package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.notify.enums.NoticeScene;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotificationDeliveryService;
import com.rally.notify.MeetupNotifyAssembler;
import com.rally.notify.NotificationEventId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 dispatch-meetup-registration-notification：报名事务提交后尽力发送对应场景通知。
 */
@Component
@RequiredArgsConstructor
public class DispatchMeetupRegistrationNotificationActivity {

    private final MeetupDomainService meetupDomainService;

    private final NotificationDeliveryService notificationDeliveryService;

    public void execute(MeetupParticipantRegistrationContext context) {
        String meetupId = context.meetupId();

        // A1-A2：每次报名只选择一个场景，并用本次报名编号构造稳定事件。
        if (RegistrationStatusEnum.JOINED == context.status()) {
            NoticeScene scene = context.full() ? NoticeScene.TEAM_SUCCESS : NoticeScene.JOIN_SUCCESS;
            List<String> recipients = context.full()
                    ? context.participantUserIds()
                    : List.of(context.userId());

            // A3-A5：领域触达命令负责提交后异步、候选去重、发送前资格复核及幂等触达。
            notificationDeliveryService.notify(
                    NotificationEventId.of(scene, context.registrationId()),
                    NotifyBizType.MEETUP,
                    meetupId,
                    scene,
                    recipients,
                    context.full()
                            ? MeetupNotifyAssembler.teamSuccessData(context.meetupData())
                            : MeetupNotifyAssembler.joinSuccessData(context.meetupData()),
                    userId -> meetupDomainService.shouldNotice(meetupId, userId));
            return;
        }

        // PENDING 只提醒发布者，不按报名成员资格过滤。
        notificationDeliveryService.notify(
                NotificationEventId.of(NoticeScene.PENDING_APPROVAL, context.registrationId()),
                NotifyBizType.MEETUP,
                meetupId,
                NoticeScene.PENDING_APPROVAL,
                List.of(context.creatorId()),
                MeetupNotifyAssembler.pendingApprovalData(
                        context.meetupData(), context.applicantNickname()));
    }
}
