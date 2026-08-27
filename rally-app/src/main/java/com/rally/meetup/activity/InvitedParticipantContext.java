package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;

import java.util.List;

/**
 * 邀请报名成功后交给群聊加入与满员通知步骤的内部上下文。
 *
 * <p>报名编号只用于构造稳定通知事件，不会作为邀请接口响应返回。</p>
 */
public record InvitedParticipantContext(
        String registrationId,
        String meetupId,
        String inviteeUserId,
        List<String> participantUserIds,
        Integer maxPlayers,
        boolean full,
        MeetupData meetupData) {

    public InvitedParticipantContext {
        participantUserIds = List.copyOf(participantUserIds);
    }
}
