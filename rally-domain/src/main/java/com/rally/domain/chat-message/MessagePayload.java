package com.rally.domain.meetup.chatmessage;

/** 聚合不解析内部格式的消息载荷。 */
public record MessagePayload(
        String content,
        ChatMessageContentType contentType) {
}
