package com.rally.domain.payment.model;

import com.rally.domain.payment.enums.PaymentViewStatus;
import lombok.Data;

/**
 * 单笔支付单对外概览（收款批次内逐人）。
 */
@Data
public class PaymentOrderSummaryDTO {
    /** 支付单号（out_trade_no） */
    private String paymentId;
    /** 约球ID */
    private String meetupId;
    /** 应付人 userId */
    private String payerUserId;
    /** 本金（分） */
    private Integer baseAmount;
    /** 手续费（分） */
    private Integer feeAmount;
    /** 实付（分） */
    private Integer payAmount;
    /** 对外支付视图态 */
    private PaymentViewStatus status;
}
