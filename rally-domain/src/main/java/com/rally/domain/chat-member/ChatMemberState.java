package com.rally.domain.meetup.chatmember;

import java.time.LocalDateTime;

/** {@code rally_meetup_chat_user} 一条当前成员记录对应的不可变状态。 */
public record ChatMemberState(
        Long id,
        String bizId,
        String refId,
        String userId,
        String lastReadMessageId,
        LocalDateTime lastReadTime,
        int unreadCount,
        LocalDateTime joinedAt,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static ChatMemberState explicitlyJoined(
            String bizId, String refId, JoinChatCommand command) {
        return new ChatMemberState(
                null,
                bizId,
                refId,
                command.userId(),
                "",
                null,
                0,
                command.joinedAt(),
                null,
                null);
    }

    static ChatMemberState repaired(
            String bizId,
            String refId,
            String userId,
            String messageId,
            LocalDateTime readAt,
            int unreadCount) {
        return new ChatMemberState(
                null,
                bizId,
                refId,
                userId,
                messageId,
                readAt,
                unreadCount,
                null,
                null,
                null);
    }

    ChatMemberState incrementUnread() {
        return withReadState(lastReadMessageId, lastReadTime, Math.addExact(unreadCount, 1));
    }

    ChatMemberState advancePosition(
            String messageId, LocalDateTime readAt, int remainingUnreadCount) {
        return withReadState(messageId, readAt, remainingUnreadCount);
    }

    private ChatMemberState withReadState(
            String messageId, LocalDateTime readTime, int remainingUnreadCount) {
        return new ChatMemberState(
                id,
                bizId,
                refId,
                userId,
                messageId,
                readTime,
                remainingUnreadCount,
                joinedAt,
                createTime,
                updateTime);
    }
}
