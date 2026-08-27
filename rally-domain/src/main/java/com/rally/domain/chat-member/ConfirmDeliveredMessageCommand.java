package com.rally.domain.meetup.chatmember;

import java.time.LocalDateTime;

/** C4 确认已交付消息命令。 */
public record ConfirmDeliveredMessageCommand(
        String userId,
        String latestMessageId,
        int messagesAfterPosition,
        LocalDateTime readAt) {
}
