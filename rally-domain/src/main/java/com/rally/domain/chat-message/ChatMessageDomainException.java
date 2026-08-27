package com.rally.domain.meetup.chatmessage;

/** 聊天消息聚合违反契约时携带稳定错误标识的领域异常。 */
public final class ChatMessageDomainException extends RuntimeException {

    private final String errorIdentifier;

    public ChatMessageDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public ChatMessageDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
