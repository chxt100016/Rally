package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付业务类型枚举（MVP 仅赛事报名费；后续退款/预收等再拓展）
 */
@Getter
@AllArgsConstructor
public enum BizTypeEnum {
    TOURNAMENT_ENTRY_FEE("赛事报名费", "Rally 赛事报名费");

    /** 业务标签（内部展示） */
    private final String label;
    /** 下单描述（透传给渠道，微信支付下单 description 字段） */
    private final String payDescription;
}
