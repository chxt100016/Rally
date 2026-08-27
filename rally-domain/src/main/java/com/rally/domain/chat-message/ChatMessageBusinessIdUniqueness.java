package com.rally.domain.meetup.chatmessage;

/** C1 在当前事务中对消息业务编号执行唯一性预检的端口。 */
@FunctionalInterface
public interface ChatMessageBusinessIdUniqueness {

    boolean exists(String messageId);
}
