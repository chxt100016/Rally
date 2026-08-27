package com.rally.domain.meetup.chatmessage;

/** 消息载荷类型及持久化编码。 */
public enum ChatMessageContentType {
    TEXT,
    IMAGE,
    LOCATION;

    public static ChatMessageContentType fromCode(String code) {
        if (code != null) {
            for (ChatMessageContentType type : values()) {
                if (type.name().equals(code)) {
                    return type;
                }
            }
        }
        throw new ChatMessageDomainException(
                ChatMessage.CHAT_MESSAGE_INVALID,
                "消息类型只能是 TEXT、IMAGE 或 LOCATION");
    }
}
