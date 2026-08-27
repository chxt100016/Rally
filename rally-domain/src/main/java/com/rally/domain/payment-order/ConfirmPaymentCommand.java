package com.rally.domain.payment.paymentorder;

import java.time.LocalDateTime;

/** C3 的渠道流水（允许为空）与调用活动生成的本地确认时间。 */
public record ConfirmPaymentCommand(String channelTransactionId, LocalDateTime channelPaidAt) {
}
