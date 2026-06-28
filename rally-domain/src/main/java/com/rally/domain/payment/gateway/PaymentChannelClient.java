package com.rally.domain.payment.gateway;

import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.model.CallbackResult;
import com.rally.domain.payment.model.ChannelShareResult;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.payment.model.SettlementOrder;
import com.rally.domain.payment.model.ShareReceiver;

import java.util.Map;

/**
 * 支付渠道网关（访问三方 → Client 后缀，见设计 §4.1）。
 * 多渠道预留：后续接支付宝新增实现，支付域与 app 编排零改动。
 */
public interface PaymentChannelClient {

    /** 当前实现对应的渠道 */
    PayChannelEnum channel();

    /** JSAPI 下单，返回小程序拉起参数 */
    PrepayResult prepay(PaymentOrder order, String payerOpenid);

    /** 关闭渠道订单（关闭收款/超时关单，best-effort） */
    void closeTrade(String outTradeNo);

    /** 查单（对账兜底） */
    ChannelTradeResult queryTrade(String outTradeNo);

    /** 添加分账接收方（分账前置） */
    void addShareReceiver(ShareReceiver receiver);

    /** 删除接收方（LRU 淘汰） */
    void deleteShareReceiver(ShareReceiver receiver);

    /** 发起分账 */
    ChannelShareResult profitShare(SettlementOrder order);

    /** 查分账结果 */
    ChannelShareResult queryProfitShare(String outOrderNo);

    /** 验签 + 解密回调 */
    CallbackResult verifyAndParse(String body, Map<String, String> headers);
}
