package com.rally.domain.social.userfollow;

/** 用户关注聚合拒绝命令时携带稳定错误标识的领域异常。 */
public final class UserFollowDomainException extends RuntimeException {

    private final String errorIdentifier;

    public UserFollowDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public UserFollowDomainException(
            String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
