package com.rally.client.pay.wechat;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.model.CallbackResult;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayResult;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAPublicKeyConfig;
import com.wechat.pay.java.core.cipher.Signer;
import com.wechat.pay.java.core.exception.ServiceException;
import com.wechat.pay.java.core.notification.NotificationConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Amount;
import com.wechat.pay.java.service.payments.jsapi.model.CloseOrderRequest;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.jsapi.model.QueryOrderByOutTradeNoRequest;
import com.wechat.pay.java.service.payments.model.Transaction;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;

/**
 * 微信支付渠道实现（V3 全套：下单 / 关单 / 查单 / 回调验签解密）。
 * <p>
 * 基于官方 SDK {@code wechatpay-java} 0.2.14；签名/敏感字段加密/回调解密全由 SDK 内部完成。
 * 商户号为「微信支付公钥」模式（非平台证书模式），使用 {@link RSAPublicKeyConfig}，
 * 验签用微信支付公钥（商户平台下载），无需下载/缓存平台证书。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WechatPayClient implements PaymentChannelClient {

    private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final WechatPayProperties properties;

    /** SDK 配置（持有商户私钥、APIv3 密钥、平台证书缓存）；初始化失败则保持 null，运行时方法 fail-fast。 */
    private Config config;
    private JsapiServiceExtension jsapiService;
    private NotificationParser notificationParser;

    @PostConstruct
    public void init() {
        if (StringUtils.isAnyBlank(properties.getMchId(), properties.getApiV3Key(), properties.getMerchantSerialNumber(), properties.getPrivateKeyPath(), properties.getPublicKeyId(), properties.getPublicKeyPath())) {
            log.warn("[WechatPayClient] 微信支付配置不完整，SDK 未初始化（dev 本地无证书可忽略）");
            return;
        }
        try {
            this.config = new RSAPublicKeyConfig.Builder().merchantId(properties.getMchId()).privateKeyFromPath(properties.getPrivateKeyPath()).merchantSerialNumber(properties.getMerchantSerialNumber()).publicKeyFromPath(properties.getPublicKeyPath()).publicKeyId(properties.getPublicKeyId()).apiV3Key(properties.getApiV3Key()).build();
            this.jsapiService = new JsapiServiceExtension.Builder().config(config).build();
            this.notificationParser = new NotificationParser((NotificationConfig) config);
            log.info("[WechatPayClient] SDK 初始化完成 mchId={}", properties.getMchId());
        } catch (Exception e) {
            log.error("[WechatPayClient] SDK 初始化失败，请检查证书与 APIv3 密钥", e);
        }
    }

    @Override
    public PayChannelEnum channel() {
        return PayChannelEnum.WECHAT;
    }

    // ==================== 支付（JSAPI 下单 / 关单 / 查单） ====================

    @Override
    public PrepayResult prepay(PaymentOrder order, String payerOpenid) {
        assertReady();
        PrepayRequest request = new PrepayRequest();
        request.setAppid(properties.getAppId());
        request.setMchid(properties.getMchId());
        request.setOutTradeNo(order.getBizId());
        request.setDescription(buildDescription(order));
        request.setNotifyUrl(properties.getPayNotifyUrl());
        if (order.getData().getExpireTime() != null) {
            request.setTimeExpire(order.getData().getExpireTime().atOffset(ZoneOffset.ofHours(8)).format(ISO_OFFSET));
        }
        Amount amount = new Amount();
        amount.setTotal(order.getData().getPayAmount());
        amount.setCurrency("CNY");
        request.setAmount(amount);
        Payer payer = new Payer();
        payer.setOpenid(payerOpenid);
        request.setPayer(payer);

        PrepayWithRequestPaymentResponse resp = jsapiService.prepayWithRequestPayment(request);
        PrepayResult result = new PrepayResult();
        result.setPrepayId(resp.getPackageVal() == null ? null : StringUtils.removeStart(resp.getPackageVal(), "prepay_id="));
        result.setTimeStamp(resp.getTimeStamp());
        result.setNonceStr(resp.getNonceStr());
        result.setPackageVal(resp.getPackageVal());
        result.setSignType(resp.getSignType());
        result.setPaySign(resp.getPaySign());
        return result;
    }

    @Override
    public PrepayResult buildRequestPayment(String prepayId) {
        assertReady();
        String appId = properties.getAppId();
        String timeStamp = String.valueOf(Instant.now().getEpochSecond());
        String nonceStr = UUID.randomUUID().toString().replace("-", "");
        String packageVal = "prepay_id=" + prepayId;
        // 小程序拉起签名：appId\ntimeStamp\nnonceStr\npackage\n，RSA-SHA256（同下单时 SDK 内部算法）
        String message = appId + "\n" + timeStamp + "\n" + nonceStr + "\n" + packageVal + "\n";
        Signer signer = config.createSigner();
        String paySign = signer.sign(message).getSign();
        PrepayResult result = new PrepayResult();
        result.setPrepayId(prepayId);
        result.setTimeStamp(timeStamp);
        result.setNonceStr(nonceStr);
        result.setPackageVal(packageVal);
        result.setSignType("RSA");
        result.setPaySign(paySign);
        return result;
    }

    @Override
    public void closeTrade(String outTradeNo) {
        assertReady();
        CloseOrderRequest request = new CloseOrderRequest();
        request.setMchid(properties.getMchId());
        request.setOutTradeNo(outTradeNo);
        try {
            jsapiService.closeOrder(request);
        } catch (ServiceException e) {
            log.warn("微信关单失败 outTradeNo={}, code={}, msg={}", outTradeNo, e.getErrorCode(), e.getErrorMessage());
        }
    }

    @Override
    public ChannelTradeResult queryTrade(String outTradeNo) {
        assertReady();
        QueryOrderByOutTradeNoRequest request = new QueryOrderByOutTradeNoRequest();
        request.setMchid(properties.getMchId());
        request.setOutTradeNo(outTradeNo);
        try {
            Transaction transaction = jsapiService.queryOrderByOutTradeNo(request);
            return toTradeResult(transaction);
        } catch (ServiceException e) {
            log.warn("微信查单失败 outTradeNo={}, code={}", outTradeNo, e.getErrorCode());
            ChannelTradeResult result = new ChannelTradeResult();
            result.setOutTradeNo(outTradeNo);
            result.setPaid(false);
            result.setTradeState(e.getErrorCode());
            return result;
        }
    }

    private ChannelTradeResult toTradeResult(Transaction transaction) {
        ChannelTradeResult result = new ChannelTradeResult();
        result.setOutTradeNo(transaction.getOutTradeNo());
        result.setChannelTransactionId(transaction.getTransactionId());
        boolean paid = transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS;
        result.setPaid(paid);
        result.setTradeState(transaction.getTradeState() == null ? null : transaction.getTradeState().name());
        return result;
    }

    // ==================== 回调验签 + 解密 ====================

    @Override
    public CallbackResult verifyAndParse(String body, Map<String, String> headers) {
        assertReady();
        RequestParam param = new RequestParam.Builder().serialNumber(get(headers, "Wechatpay-Serial")).nonce(get(headers, "Wechatpay-Nonce")).signature(get(headers, "Wechatpay-Signature")).timestamp(get(headers, "Wechatpay-Timestamp")).body(body).build();

        @SuppressWarnings("unchecked")
        Map<String, Object> envelope = JSON.parseObject(body, Map.class);
        String eventType = envelope == null ? "" : String.valueOf(envelope.getOrDefault("event_type", ""));

        CallbackResult result = new CallbackResult();
        if (StringUtils.startsWithIgnoreCase(eventType, "TRANSACTION")) {
            Transaction transaction = notificationParser.parse(param, Transaction.class);
            result.setCallbackType("TRANSACTION");
            result.setOutTradeNo(transaction.getOutTradeNo());
            result.setChannelTransactionId(transaction.getTransactionId());
            result.setSuccess(transaction.getTradeState() == Transaction.TradeStateEnum.SUCCESS);
            result.setDecryptedBody(JSON.toJSONString(transaction));
        } else {
            log.warn("未知微信回调 event_type={}", eventType);
            result.setCallbackType("UNKNOWN");
            result.setDecryptedBody(body);
        }
        return result;
    }

    private String get(Map<String, String> headers, String name) {
        if (headers == null) {
            return null;
        }
        String v = headers.get(name);
        return v != null ? v : headers.get(name.toLowerCase());
    }

    // ==================== 辅助 ====================

    private void assertReady() {
        if (jsapiService == null || notificationParser == null) {
            log.error("WechatPayClient SDK 未就绪，配置缺失或初始化失败");
            throw new BusinessException(BizErrorCode.PAYMENT_CHANNEL_NOT_SUPPORTED);
        }
    }

    private String buildDescription(PaymentOrder order) {
        String desc = order.getData().getDescription();
        if (StringUtils.isNotBlank(desc)) {
            return desc;
        }
        return order.getData().getBizType().getPayDescription();
    }
}
