package com.rally.domain.payment.model;

import lombok.Data;

/**
 * 渠道回调验签 + 解密后的结果。
 */
@Data
public class CallbackResult {
    /** 回调类型：TRANSACTION 支付 / PROFIT_SHARE 分账 */
    private String callbackType;
    /** 我方单号（out_trade_no / out_order_no） */
    private String outTradeNo;
    private String channelTransactionId;
    /** 是否支付/分账成功 */
    private boolean success;
    /** 解密后的原始报文，落 payment_log */
    private String decryptedBody;
}
