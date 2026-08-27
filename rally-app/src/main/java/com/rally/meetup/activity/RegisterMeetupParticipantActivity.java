package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 业务活动 register-meetup-participant：校验准入、建立报名并返回后续步骤所需上下文。
 */
@Component
@RequiredArgsConstructor
public class RegisterMeetupParticipantActivity {

    private final UserProfileDomainService userProfileDomainService;

    private final MeetupDomainService meetupDomainService;

    private final RegistrationDomainService registrationDomainService;

    public MeetupParticipantRegistrationContext execute(
            String meetupId, String userId, LocalDateTime autoWithdrawAt) {
        // A1：账户、基础资料和网球档案完整性沿用用户聚合既有错误语义。
        UserProfile userProfile = userProfileDomainService.get(userId);
        userProfile.assertCompleted();

        // A2-A3：完整加载约球及报名，状态、容量、既有关系和准入由聚合命令统一校验。
        Meetup meetup = meetupDomainService.get(meetupId);

        // A4-A5：按加入模式建立报名，原样保存自动撤回时间并整体持久化、重算人数。
        RegistrationStatusEnum status = registrationDomainService.join(meetup, userProfile, autoWithdrawAt);
        RegistrationData registration = meetup.findActiveRegistration(userId);

        return new MeetupParticipantRegistrationContext(
                registration.getBizId(),
                status,
                meetupId,
                userId,
                meetup.getCreatorId(),
                userProfile.getUser().getNickname(),
                meetup.getActiveParticipantIds(null),
                meetup.getData().getMaxPlayers(),
                meetup.isFull(),
                meetup.getData());
    }
}
