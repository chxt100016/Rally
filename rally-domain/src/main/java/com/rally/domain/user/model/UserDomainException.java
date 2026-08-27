package com.rally.domain.user.model;

/**
 * 用户聚合违反 Blueprint 契约时抛出的领域异常。
 *
 * <p>错误标识独立于传输层错误码，便于仓储把数据库唯一键冲突映射回稳定的领域语义。</p>
 */
public class UserDomainException extends RuntimeException {

    private final String errorIdentifier;

    public UserDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public UserDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
