package com.rally.domain.payment.paymentorder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** C1 建立支付单的确定输入；费率和超时时间已由应用层决定。 */
public record CreatePaymentOrderCommand(
        String channel,
        String bizType,
        String refBizId,
        String payerUserId,
        int baseAmount,
        BigDecimal feeRate,
        String description,
        LocalDateTime expireTime,
        LocalDateTime createdAt) {
}
