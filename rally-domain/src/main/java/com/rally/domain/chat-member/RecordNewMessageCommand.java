package com.rally.domain.meetup.chatmember;

/** C3 记录频道新消息命令。 */
public record RecordNewMessageCommand(
        String messageId,
        String senderUserId,
        int messagesAfterPosition) {
}
