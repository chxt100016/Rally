package com.rally.domain.system.platformconfig;

/** 平台配置聚合违反领域契约时抛出的稳定领域异常。 */
public final class PlatformConfigDomainException extends RuntimeException {

    private final String errorIdentifier;

    public PlatformConfigDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public PlatformConfigDomainException(
            String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
