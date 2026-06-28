package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付渠道枚举（name 大写）
 */
@Getter
@AllArgsConstructor
public enum PayChannelEnum {
    WECHAT("微信支付");

    private final String label;
}
