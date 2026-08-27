package com.rally.domain.payment.paymentorder;

import java.time.LocalDateTime;

/** 与 {@code payment_order} 一一对应的支付单状态快照。 */
public record PaymentOrderState(
        Long id,
        String bizId,
        String channel,
        String bizType,
        String refBizId,
        String payerUserId,
        int baseAmount,
        int feeAmount,
        int payAmount,
        PaymentOrderStatus status,
        String channelTransactionId,
        String prepayId,
        LocalDateTime prepayExpireTime,
        String activeRefKey,
        String description,
        LocalDateTime payTime,
        LocalDateTime expireTime,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public PaymentOrderState withPrepay(String newPrepayId, LocalDateTime newPrepayExpireTime) {
        return new PaymentOrderState(
                id, bizId, channel, bizType, refBizId, payerUserId,
                baseAmount, feeAmount, payAmount, status, channelTransactionId,
                newPrepayId, newPrepayExpireTime, activeRefKey, description,
                payTime, expireTime, createTime, updateTime);
    }

    public PaymentOrderState asPaid(String transactionId, LocalDateTime paidAt) {
        return new PaymentOrderState(
                id, bizId, channel, bizType, refBizId, payerUserId,
                baseAmount, feeAmount, payAmount, PaymentOrderStatus.PAID, transactionId,
                prepayId, prepayExpireTime, activeRefKey, description,
                paidAt, expireTime, createTime, updateTime);
    }

    public PaymentOrderState asClosed() {
        return terminal(PaymentOrderStatus.CLOSED, description);
    }

    public PaymentOrderState asFailed(String failureSummary) {
        return terminal(PaymentOrderStatus.FAILED, failureSummary);
    }

    private PaymentOrderState terminal(PaymentOrderStatus terminalStatus, String terminalDescription) {
        return new PaymentOrderState(
                id, bizId, channel, bizType, refBizId, payerUserId,
                baseAmount, feeAmount, payAmount, terminalStatus, null,
                prepayId, prepayExpireTime, null, terminalDescription,
                null, expireTime, createTime, updateTime);
    }
}
