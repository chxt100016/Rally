package com.rally.domain.payment.paymentorder;

/** 支付单聚合违反契约时抛出的领域异常。 */
public final class PaymentOrderDomainException extends RuntimeException {

    private final String errorIdentifier;

    public PaymentOrderDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public PaymentOrderDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
