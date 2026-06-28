package com.rally.domain.payment.model;

import lombok.Data;

/**
 * 渠道查单结果（对账兜底用）。
 */
@Data
public class ChannelTradeResult {
    private String outTradeNo;
    private String channelTransactionId;
    /** 是否已支付成功 */
    private boolean paid;
    /** 渠道原始交易状态 */
    private String tradeState;
}
