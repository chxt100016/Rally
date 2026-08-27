package com.rally.domain.account.model;

/** 账户聚合违反契约时抛出的领域异常，保留 Blueprint 定义的稳定错误标识。 */
public class AccountDomainException extends RuntimeException {

    private final String errorIdentifier;

    public AccountDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public AccountDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
