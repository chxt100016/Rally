package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 分账接收方绑定状态：BOUND 已添加 / UNBOUND 已删除（淘汰）
 */
@Getter
@AllArgsConstructor
public enum ShareReceiverStatusEnum {
    BOUND("已绑定"),
    UNBOUND("已解绑");

    private final String label;
}
