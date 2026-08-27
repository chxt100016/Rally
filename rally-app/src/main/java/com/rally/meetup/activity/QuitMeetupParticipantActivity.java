package com.rally.meetup.activity;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.meetup.model.QuitResult;
import com.rally.domain.meetup.model.RegistrationData;
import com.rally.domain.meetup.service.MeetupDomainService;
import com.rally.domain.meetup.service.RegistrationDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 quit-meetup-participant：退出普通约球并返回后续群聊和通知所需上下文。
 */
@Component
@RequiredArgsConstructor
public class QuitMeetupParticipantActivity {

    private final MeetupDomainService meetupDomainService;

    private final RegistrationDomainService registrationDomainService;

    public MeetupParticipantQuitContext execute(String meetupId, String userId) {
        // A1-A2：完整加载约球与报名；赛事类型和有效报名状态由聚合命令统一校验。
        Meetup meetup = meetupDomainService.get(meetupId);
        RegistrationData quittingRegistration = meetup.findActiveRegistration(userId);

        // A3-A4：聚合命令判断临近开始处罚、置为 QUIT，并整体保存、重算当前人数。
        QuitResult quitResult = registrationDomainService.quit(meetup, userId);

        // A5：只返回同事务下游删除群聊和提交后通知所需的内部上下文。
        return new MeetupParticipantQuitContext(
                quittingRegistration.getBizId(),
                meetupId,
                userId,
                meetup.getCreatorId(),
                meetup.getData().getCurrentPlayers(),
                quitResult,
                meetup.getData());
    }
}
