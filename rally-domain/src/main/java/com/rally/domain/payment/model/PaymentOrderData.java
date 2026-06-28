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
    private String collectionBatchId;
    private String meetupId;
    private String payerUserId;
    private String payeeUserId;
    /** 收款受益人渠道账号（微信 openid），冗余进单避免分账时再次反查 */
    private String payeeAccount;
    private Integer baseAmount;
    private Integer feeAmount;
    private Integer payAmount;
    private PaymentStatusEnum status;
    private String channelTransactionId;
    private String prepayId;
    private String description;
    private LocalDateTime payTime;
    private LocalDateTime expireTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
