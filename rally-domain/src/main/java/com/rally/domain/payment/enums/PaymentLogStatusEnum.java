package com.rally.domain.payment.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 支付留痕处理状态：RECEIVED 待处理 / PROCESSED 已处理 / FAILED 处理失败。
 * COLLECT/PREPAY 落库即 PROCESSED；仅 CALLBACK 落 RECEIVED 待处理，供补偿扫描。
 */
@Getter
@AllArgsConstructor
public enum PaymentLogStatusEnum {
    RECEIVED("待处理"),
    PROCESSED("已处理"),
    FAILED("处理失败");

    private final String label;
}
