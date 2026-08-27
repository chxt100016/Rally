package com.rally.meetup.activity;

import com.rally.domain.meetup.model.MeetupData;
import com.rally.domain.meetup.model.QuitResult;

/**
 * 参与者退出后供群聊和通知步骤使用的内部上下文；不会作为接口响应返回。
 */
public record MeetupParticipantQuitContext(
        String registrationId,
        String meetupId,
        String userId,
        String creatorId,
        Integer currentPlayers,
        QuitResult quitResult,
        MeetupData meetupData) {
}
