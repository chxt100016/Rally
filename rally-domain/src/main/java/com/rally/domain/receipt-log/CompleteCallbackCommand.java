package com.rally.domain.payment.receiptlog;

import com.rally.domain.payment.enums.PaymentLogStatusEnum;

/** C2 以一个确定的处理结论终结 CALLBACK。 */
public record CompleteCallbackCommand(
        PaymentLogStatusEnum conclusion,
        String remark) {
}
