package com.rally.db.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 支付单/收款流水 PO
 */
@Data
@TableName("payment_order")
public class PaymentOrderPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String channel;
    private String bizType;
    private String refBizId;
    private String payerUserId;
    private Integer baseAmount;
    private Integer feeAmount;
    private Integer payAmount;
    private String status;
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
