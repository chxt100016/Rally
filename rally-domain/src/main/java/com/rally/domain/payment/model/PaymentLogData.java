package com.rally.domain.payment.model;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentLogStatusEnum;
import com.rally.domain.payment.enums.PaymentLogTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付全链路留痕领域数据对象（与 payment_log 同构，非聚合根）
 */
@Data
public class PaymentLogData {
    private String bizId;
    private PayChannelEnum channel;
    private PaymentLogTypeEnum logType;
    private String refType;
    private String refId;
    private String rawBody;
    private PaymentLogStatusEnum processStatus;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
