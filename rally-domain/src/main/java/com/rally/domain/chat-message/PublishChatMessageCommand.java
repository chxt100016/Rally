package com.rally.domain.meetup.chatmessage;

import java.time.LocalDateTime;

/** C1 发布消息命令入参。 */
public record PublishChatMessageCommand(
        String refId,
        String senderId,
        String senderName,
        String senderAvatarKey,
        String content,
        String contentType,
        LocalDateTime publishedAt) {
}
