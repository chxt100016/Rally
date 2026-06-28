package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 发起人收款入口态（详情页，见设计 §5.6）：
 * INITIABLE 无单→「发起收款」/ ONGOING 有 PENDING→「关闭收款」/ ENDED 有单无 PENDING→「收款已结束」
 */
@Getter
@AllArgsConstructor
public enum CollectionStateEnum {
    INITIABLE("可发起收款"),
    ONGOING("收款进行中"),
    ENDED("收款已结束");

    private final String label;
}
