package com.rally.domain.meetup.chatmessage;

import java.time.LocalDateTime;

/** {@code rally_meetup_chat_message} 一条已发布消息的不可变状态快照。 */
public record ChatMessageData(
        Long id,
        String bizId,
        String refId,
        String senderId,
        String senderName,
        String senderAvatar,
        String content,
        ChatMessageContentType contentType,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static ChatMessageData newPublished(
            String bizId,
            PublishChatMessageCommand command,
            ChatMessageContentType contentType) {
        return new ChatMessageData(
                null,
                bizId,
                command.refId(),
                command.senderId(),
                nullToEmpty(command.senderName()),
                nullToEmpty(command.senderAvatarKey()),
                command.content(),
                contentType,
                command.publishedAt(),
                command.publishedAt());
    }

    public SenderSnapshot senderSnapshot() {
        return new SenderSnapshot(senderId, senderName, senderAvatar);
    }

    public MessagePayload payload() {
        return new MessagePayload(content, contentType);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
