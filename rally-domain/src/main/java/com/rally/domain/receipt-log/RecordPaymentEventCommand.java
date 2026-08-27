package com.rally.domain.payment.receiptlog;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;

/** C1 记录一条已完成验真（如适用）的支付事件。 */
public record RecordPaymentEventCommand(
        PayChannelEnum channel,
        PaymentLogTypeEnum logType,
        ReceiptLogReference reference,
        String rawBody) {
}
