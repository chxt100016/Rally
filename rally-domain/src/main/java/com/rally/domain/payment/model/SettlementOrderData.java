package com.rally.domain.payment.model;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.enums.SettlementStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分账单领域数据对象（与 payment_settlement 同构）
 */
@Data
public class SettlementOrderData {
    private String bizId;
    private String paymentOrderId;
    private PayChannelEnum channel;
    private String channelTransactionId;
    private String meetupId;
    private String payeeUserId;
    private String payeeAccount;
    private Integer shareAmount;
    private SettlementStatusEnum status;
    private String channelOrderId;
    private String failReason;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
