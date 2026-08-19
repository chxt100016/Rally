package com.rally.domain.user.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserExtKeyEnum {
    /** ext_key 保留历史值，避免已有用户收款码失效。 */
    PAYMENT_CODE("wechat_payment_code", "用户收款码");

    private final String key;
    private final String description;
}
