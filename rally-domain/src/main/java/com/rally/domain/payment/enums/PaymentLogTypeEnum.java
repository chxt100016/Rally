package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付全链路留痕类型：
 * COLLECT 发起收款建单（纯留痕，落库即 PROCESSED）
 * PREPAY  参与人发起支付下单（纯留痕，落库即 PROCESSED）
 * CALLBACK 渠道回调（落 RECEIVED 待处理，仅此类型有推进语义）
 */
@Getter
@AllArgsConstructor
public enum PaymentLogTypeEnum {
    COLLECT("发起收款建单"),
    PREPAY("发起支付下单"),
    CALLBACK("渠道回调");

    private final String label;
}
