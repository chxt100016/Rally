package com.rally.meetup.activity;

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
 * 业务活动 dispatch-registration-approved-notification：审批事务提交后尽力发送对应场景通知。
 */
@Component
@RequiredArgsConstructor
public class DispatchRegistrationApprovedNotificationActivity {

    private final MeetupDomainService meetupDomainService;

    private final NotificationDeliveryService notificationDeliveryService;

    public void execute(ApprovedRegistrationContext context) {
        String meetupId = context.meetupId();

        // A1-A2：达到或超过人数上限时只发组团成功，否则只向获批人发报名成功；
        // 两种场景均以本次获批报名编号形成稳定事件。
        NoticeScene scene = context.fullAfterApproval()
                ? NoticeScene.TEAM_SUCCESS
                : NoticeScene.JOIN_SUCCESS;
        List<String> recipients = context.fullAfterApproval()
                ? context.participantUserIds()
                : List.of(context.approvedUserId());

        // A3-A5：领域触达命令负责提交后异步、候选去重、发送前资格复核及幂等微信触达。
        notificationDeliveryService.notify(
                NotificationEventId.of(scene, context.registrationId()),
                NotifyBizType.MEETUP,
                meetupId,
                scene,
                recipients,
                context.fullAfterApproval()
                        ? MeetupNotifyAssembler.teamSuccessData(context.meetupData())
                        : MeetupNotifyAssembler.joinSuccessData(context.meetupData()),
                userId -> meetupDomainService.shouldNotice(meetupId, userId));
    }
}
