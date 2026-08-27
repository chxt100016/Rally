package com.rally.domain.payment.paymentorder;

import java.time.LocalDateTime;

/** C2 保存渠道预支付凭证；字段按调用方结果原样传递，currentTime 仅保留既有契约。 */
public record SavePrepayCommand(
        String prepayId,
        LocalDateTime prepayExpireTime,
        LocalDateTime currentTime) {
}
