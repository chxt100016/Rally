package com.rally.domain.identity.userextension;

/** 用户扩展资料命令违反聚合不变量时抛出的领域异常。 */
public final class UserExtensionDomainException extends RuntimeException {

    private final UserExtensionError error;

    public UserExtensionDomainException(UserExtensionError error, String message) {
        super(message);
        this.error = error;
    }

    public UserExtensionDomainException(UserExtensionError error, String message, Throwable cause) {
        super(message, cause);
        this.error = error;
    }

    public UserExtensionError getError() {
        return error;
    }

    public String getErrorIdentifier() {
        return error.name();
    }
}
