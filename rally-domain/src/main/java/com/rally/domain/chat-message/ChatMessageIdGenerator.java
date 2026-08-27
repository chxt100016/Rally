package com.rally.domain.meetup.chatmessage;

/**
 * 每次发布时生成全局唯一、固定长度且按时间有序的十进制雪花消息编号。
 */
@FunctionalInterface
public interface ChatMessageIdGenerator {

    String nextMessageId();
}
