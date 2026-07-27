package com.rally.domain.payment.model;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.PaymentStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付单领域数据对象（与 payment_order 同构，跨层传递）
 */
@Data
public class PaymentOrderData {
    private String bizId;
    private PayChannelEnum channel;
    private BizTypeEnum bizType;
    private String refBizId;
    private String payerUserId;
    private Integer baseAmount;
    private Integer feeAmount;
    private Integer payAmount;
    private PaymentStatusEnum status;
    private String channelTransactionId;
    private String prepayId;
    private LocalDateTime prepayExpireTime;
    private String activeRefKey;
    private String description;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
