package com.rally.db.payment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分账单 PO
 */
@Data
@TableName("payment_settlement")
public class SettlementOrderPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String paymentOrderId;
    private String channel;
    private String channelTransactionId;
    private String meetupId;
    private String payeeUserId;
    private String payeeAccount;
    private Integer shareAmount;
    private String status;
    private String channelOrderId;
    private String failReason;
    private LocalDateTime finishTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
