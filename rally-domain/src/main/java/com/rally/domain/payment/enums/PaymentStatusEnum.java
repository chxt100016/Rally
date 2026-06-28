package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付单状态机：PENDING → PAID，或 PENDING → CLOSED（超时/关闭），PENDING → FAILED
 */
@Getter
@AllArgsConstructor
public enum PaymentStatusEnum {
    PENDING("待支付"),
    PAID("已支付"),
    CLOSED("已关闭"),
    FAILED("已失败");

    private final String label;
}
