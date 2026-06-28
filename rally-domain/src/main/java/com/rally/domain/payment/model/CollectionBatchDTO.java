package com.rally.domain.payment.model;

import lombok.Data;

import java.util.List;

/**
 * 发起收款返回的收款批次概览。
 */
@Data
public class CollectionBatchDTO {
    /** 收款批次ID */
    private String batchId;
    /** 约球ID */
    private String meetupId;
    /** 总额（分） */
    private Integer totalAmount;
    /** 应付人数 */
    private Integer payerCount;
    /** 手续费展示文案 */
    private String feeDesc;
    /** 逐人支付单概览 */
    private List<PaymentOrderSummaryDTO> orders;
}
