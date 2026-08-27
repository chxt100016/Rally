package com.rally.domain.delivery;

/** 触达聚合违反不变量时使用的稳定领域错误。 */
public final class DeliveryDomainException extends RuntimeException {

    private final String errorIdentifier;

    public DeliveryDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
