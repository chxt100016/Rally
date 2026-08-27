package com.rally.meetup.activity;

import com.rally.domain.meetup.enums.RegistrationStatusEnum;
import com.rally.domain.meetup.model.MeetupData;

import java.util.List;

/**
 * 报名建立后交给群聊与通知步骤的内部上下文。
 *
 * <p>报名编号只用于构造稳定通知事件，不会作为报名接口响应返回。</p>
 */
public record MeetupParticipantRegistrationContext(
        String registrationId,
        RegistrationStatusEnum status,
        String meetupId,
        String userId,
        String creatorId,
        String applicantNickname,
        List<String> participantUserIds,
        Integer maxPlayers,
        boolean full,
        MeetupData meetupData) {

    public MeetupParticipantRegistrationContext {
        participantUserIds = List.copyOf(participantUserIds);
    }
}
