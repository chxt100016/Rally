package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付业务类型枚举（MVP 仅活动后发起收款；后续退款/预收等再拓展）
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum {
    MEETUP_COLLECT("活动收款"),
    TOURNAMENT_ENTRY_FEE("赛事报名费");

    private final String label;
}
