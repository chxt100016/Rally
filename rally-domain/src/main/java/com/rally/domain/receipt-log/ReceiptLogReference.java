package com.rally.domain.payment.receiptlog;

/** payment_log 的可选关联对象；当前契约只允许关联支付单。 */
public record ReceiptLogReference(String refType, String refId) {

    public static final String PAYMENT_ORDER = "ORDER";

    public static ReceiptLogReference none() {
        return new ReceiptLogReference(null, null);
    }

    public static ReceiptLogReference paymentOrder(String paymentOrderId) {
        return new ReceiptLogReference(PAYMENT_ORDER, paymentOrderId);
    }
}
