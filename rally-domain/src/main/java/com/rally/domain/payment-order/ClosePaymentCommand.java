package com.rally.domain.payment.paymentorder;

import java.time.LocalDateTime;

/** C4 关闭意图；调用活动负责到期或主动关闭资格判断。 */
public record ClosePaymentCommand(LocalDateTime currentTime, boolean activelyAllowed) {
}
