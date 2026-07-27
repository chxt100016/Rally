package com.rally.domain.payment.model;

import lombok.Data;

/**
 * 支付拉起参数对外返回：支付单标识 + 小程序 wx.requestPayment 入参。
 * 前端拿 paymentId 轮询 sync-status 确认到账，拿其余字段调起微信支付。
 */
@Data
public class PrepayDTO {
    /** 支付单标识（out_trade_no），前端用于轮询 sync-status */
    private String paymentId;
    private String prepayId;
    private String timeStamp;
    private String nonceStr;
    /** 微信小程序为 "prepay_id=xxx" 形式 */
    private String packageVal;
    private String signType;
    private String paySign;
}
