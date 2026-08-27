package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 register-invited-participant：登记被邀请人并返回后续步骤所需上下文。
 */
@Component
@RequiredArgsConstructor
public class RegisterInvitedParticipantActivity {

    private final MeetupDomainService meetupDomainService;

    private final RegistrationDomainService registrationDomainService;

    public InvitedParticipantContext execute(String meetupId, String inviterId, String inviteeUserId) {
        // A1-A2：完整加载约球与全部报名；创建者、容量和活跃报名检查由聚合命令完成。
        Meetup meetup = meetupDomainService.get(meetupId);

        // A3-A4：聚合命令保留终止历史，新建 JOINED 报名并整体保存、重算当前人数。
        registrationDomainService.invite(meetup, inviteeUserId, inviterId);
        RegistrationData registration = meetup.findActiveRegistration(inviteeUserId);

        // A5：报名编号仅供稳定通知事件使用，接口层仍不返回报名信息。
        return new InvitedParticipantContext(
                registration.getBizId(),
                meetupId,
                inviteeUserId,
                meetup.getActiveParticipantIds(null),
                meetup.getData().getMaxPlayers(),
                meetup.isFull(),
                meetup.getData());
    }
}
