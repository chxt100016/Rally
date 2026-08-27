package com.rally.domain.profilechangelog.model;

/** 档案变更日志违反契约时抛出的领域异常。 */
public class ProfileChangeLogDomainException extends RuntimeException {

    private final String errorIdentifier;

    public ProfileChangeLogDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public ProfileChangeLogDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
