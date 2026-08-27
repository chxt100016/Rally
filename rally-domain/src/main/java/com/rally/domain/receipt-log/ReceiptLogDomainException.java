package com.rally.domain.payment.receiptlog;

/** 支付回执日志聚合违反领域契约时抛出的异常。 */
public final class ReceiptLogDomainException extends RuntimeException {

    private final String errorIdentifier;

    public ReceiptLogDomainException(String errorIdentifier, String message) {
        super(message);
        this.errorIdentifier = errorIdentifier;
    }

    public ReceiptLogDomainException(String errorIdentifier, String message, Throwable cause) {
        super(message, cause);
        this.errorIdentifier = errorIdentifier;
    }

    public String getErrorIdentifier() {
        return errorIdentifier;
    }
}
