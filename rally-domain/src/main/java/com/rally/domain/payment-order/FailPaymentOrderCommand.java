package com.rally.domain.payment.paymentorder;

/** C5 标记建单失败；失败摘要按调用方结果原样传递。 */
public record FailPaymentOrderCommand(String failureSummary) {
}
