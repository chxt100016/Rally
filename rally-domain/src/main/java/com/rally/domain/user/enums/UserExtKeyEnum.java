package com.rally.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserExtKeyEnum {
    WECHAT_PAYMENT_CODE("wechat_payment_code", "微信付款码");

    private final String key;
    private final String description;
}
