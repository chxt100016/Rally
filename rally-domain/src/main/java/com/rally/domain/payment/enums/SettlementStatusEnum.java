package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分账单状态机：PENDING → PROCESSING → FINISHED / FAILED
 */
@Getter
@AllArgsConstructor
public enum SettlementStatusEnum {
    PENDING("待分账"),
    PROCESSING("分账中"),
    FINISHED("已完成"),
    FAILED("已失败");

    private final String label;
}
