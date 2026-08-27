package com.rally.domain.payment.receiptlog;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;

import java.time.LocalDateTime;

/** 与 payment_log 一行数据对应的不可变聚合快照。 */
public record ReceiptLogState(
        Long id,
        String bizId,
        PayChannelEnum channel,
        PaymentLogTypeEnum logType,
        ReceiptLogReference reference,
        String rawBody,
        PaymentLogStatusEnum processStatus,
        String remark,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    public ReceiptLogState withConclusion(
            PaymentLogStatusEnum conclusion, String newRemark) {
        return new ReceiptLogState(
                id,
                bizId,
                channel,
                logType,
                reference,
                rawBody,
                conclusion,
                newRemark,
                createTime,
                updateTime);
    }
}
