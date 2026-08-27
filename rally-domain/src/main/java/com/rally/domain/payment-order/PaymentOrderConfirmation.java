package com.rally.domain.payment.paymentorder;

/** C3 按更新前加载状态得出的幂等确认结论。 */
public enum PaymentOrderConfirmation {
    FIRST_PAID,
    ALREADY_PAID
}
