package com.rally.meetup.activity;

import java.time.LocalDateTime;

/**
 * 成员退出通知所需的稳定业务事件与语义化内容。
 */
public record MemberQuitNotificationContext(
        String registrationId,
        String meetupId,
        String creatorId,
        String meetupName,
        LocalDateTime startTime,
        String quitUserNickname,
        LocalDateTime quitTime) {
}
