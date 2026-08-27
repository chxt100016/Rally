package com.rally.domain.meetup.chatmessage;

/** 发布时固化的发送者编号与展示资料。 */
public record SenderSnapshot(
        String senderId,
        String senderName,
        String senderAvatarKey) {
}
