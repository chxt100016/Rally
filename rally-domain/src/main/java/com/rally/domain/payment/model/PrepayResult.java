package com.rally.domain.payment.model;

import lombok.Data;

/**
 * 渠道下单返回的小程序拉起参数（JSAPI）。
 * 字段对齐微信 wx.requestPayment 入参，前端直接使用。
 */
@Data
public class PrepayResult {
    private String prepayId;
    private String timeStamp;
    private String nonceStr;
    /** 微信小程序为 "prepay_id=xxx" 形式 */
    private String packageVal;
    private String signType;
    private String paySign;
}
