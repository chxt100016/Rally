package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 对外支付视图态（独立于内部 {@link PaymentStatusEnum}，避免内部态外泄，见设计 §5.5）
 */
@Getter
@AllArgsConstructor
public enum PaymentViewStatus {
    NONE("未发起收款"),
    UNPAID("待支付"),
    PAID("已支付"),
    CLOSED("已取消");

    private final String label;
}
