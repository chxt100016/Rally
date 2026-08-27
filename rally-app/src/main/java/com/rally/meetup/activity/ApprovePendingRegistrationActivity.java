package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 approve-pending-registration：批准待审报名并返回后续步骤所需上下文。
 */
@Component
@RequiredArgsConstructor
public class ApprovePendingRegistrationActivity {

    private final MeetupDomainService meetupDomainService;

    private final RegistrationDomainService registrationDomainService;

    public ApprovedRegistrationContext execute(String meetupId, String registrationId, String approverId) {
        // A1-A2：完整加载约球与全部报名；报名归属、创建者、PENDING 和活跃状态由聚合命令校验。
        Meetup meetup = meetupDomainService.get(meetupId);

        // A3-A4：批准不检查容量或过期时间，整体保存后按全部有效报名如实重算人数。
        String approvedUserId = registrationDomainService.approve(meetup, registrationId, approverId);

        return new ApprovedRegistrationContext(
                registrationId,
                meetupId,
                approvedUserId,
                meetup.getActiveParticipantIds(null),
                meetup.getData().getMaxPlayers(),
                meetup.isFull(),
                meetup.getData());
    }
}
