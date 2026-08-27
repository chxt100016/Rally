package com.rally.domain.payment.receiptlog;

import com.rally.domain.payment.enums.PaymentLogStatusEnum;

/**
 * payment_log 的唯一写端口。
 *
 * <p>适配器执行完整 insert，以及只按 {@code biz_id=?}
 * 定位的普通 update；更新不附加原状态条件，也不依据影响行数重试或补查。</p>
 */
public interface ReceiptLogPersistence {

    ReceiptLogInsertResult insert(ReceiptLogState state);

    void updateConclusion(
            String bizId, PaymentLogStatusEnum conclusion, String remark);
}
