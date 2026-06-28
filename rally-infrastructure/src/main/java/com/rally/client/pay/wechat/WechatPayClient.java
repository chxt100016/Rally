package com.rally.client.pay.wechat;

import com.alibaba.fastjson2.JSON;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.model.CallbackResult;
import com.rally.domain.payment.model.ChannelShareResult;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.payment.model.SettlementOrder;
import com.rally.domain.payment.model.ShareReceiver;
import com.wechat.pay.java.core.Config;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
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
import com.wechat.pay.java.service.profitsharing.ProfitsharingService;
import com.wechat.pay.java.service.profitsharing.model.AddReceiverRequest;
import com.wechat.pay.java.service.profitsharing.model.CreateOrderReceiver;
import com.wechat.pay.java.service.profitsharing.model.CreateOrderRequest;
import com.wechat.pay.java.service.profitsharing.model.DeleteReceiverRequest;
import com.wechat.pay.java.service.profitsharing.model.OrderStatus;
import com.wechat.pay.java.service.profitsharing.model.OrdersEntity;
import com.wechat.pay.java.service.profitsharing.model.QueryOrderRequest;
import com.wechat.pay.java.service.profitsharing.model.ReceiverRelationType;
import com.wechat.pay.java.service.profitsharing.model.ReceiverType;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 微信支付渠道实现（V3 全套：下单 / 关单 / 查单 / 回调验签解密 / 添加接收方 / 删除接收方 / 发起分账 / 查询分账）。
 * <p>
 * 基于官方 SDK {@code wechatpay-java} 0.2.14；签名/敏感字段加密/回调解密全由 SDK 内部完成，
 * 商户证书自动下载（{@link RSAAutoCertificateConfig}），无需手动维护。
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
    private ProfitsharingService profitsharingService;
    private NotificationParser notificationParser;

    @PostConstruct
    public void init() {
        if (StringUtils.isAnyBlank(properties.getMchId(), properties.getApiV3Key(), properties.getMerchantSerialNumber(), properties.getPrivateKeyPath())) {
            log.warn("[WechatPayClient] 微信支付配置不完整，SDK 未初始化（dev 本地无证书可忽略）");
            return;
        }
        try {
            this.config = new RSAAutoCertificateConfig.Builder().merchantId(properties.getMchId()).privateKeyFromPath(properties.getPrivateKeyPath()).merchantSerialNumber(properties.getMerchantSerialNumber()).apiV3Key(properties.getApiV3Key()).build();
            this.jsapiService = new JsapiServiceExtension.Builder().config(config).build();
            this.profitsharingService = new ProfitsharingService.Builder().config(config).build();
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

    // ==================== 分账接收方（添加 / 删除） ====================

    @Override
    public void addShareReceiver(ShareReceiver receiver) {
        assertReady();
        AddReceiverRequest request = new AddReceiverRequest();
        request.setAppid(properties.getAppId());
        request.setType(ReceiverType.PERSONAL_OPENID);
        request.setAccount(receiver.getData().getAccount());
        // 与发起人关系：USER（个人 openid 场景默认）。微信 V3 要求 relation_type 必填
        request.setRelationType(ReceiverRelationType.USER);
        try {
            profitsharingService.addReceiver(request);
        } catch (ServiceException e) {
            // 已存在视为成功（接收方账本本地保证幂等，远端"已添加"等价于绑定成功）
            if (isAlreadyExists(e)) {
                log.info("分账接收方已存在视为成功 openid={}", receiver.getData().getAccount());
                return;
            }
            log.error("添加分账接收方失败 openid={}, code={}, msg={}", receiver.getData().getAccount(), e.getErrorCode(), e.getErrorMessage());
            throw new BusinessException(BizErrorCode.SHARE_RECEIVER_BIND_FAILED);
        }
    }

    @Override
    public void deleteShareReceiver(ShareReceiver receiver) {
        assertReady();
        DeleteReceiverRequest request = new DeleteReceiverRequest();
        request.setAppid(properties.getAppId());
        request.setType(ReceiverType.PERSONAL_OPENID);
        request.setAccount(receiver.getData().getAccount());
        try {
            profitsharingService.deleteReceiver(request);
        } catch (ServiceException e) {
            log.warn("删除分账接收方失败（忽略） openid={}, code={}, msg={}", receiver.getData().getAccount(), e.getErrorCode(), e.getErrorMessage());
        }
    }

    private boolean isAlreadyExists(ServiceException e) {
        String code = e.getErrorCode();
        return StringUtils.containsIgnoreCase(code, "EXIST") || StringUtils.containsIgnoreCase(e.getErrorMessage(), "已存在");
    }

    // ==================== 分账（发起 / 查询） ====================

    @Override
    public ChannelShareResult profitShare(SettlementOrder order) {
        assertReady();
        CreateOrderRequest request = new CreateOrderRequest();
        request.setAppid(properties.getAppId());
        request.setTransactionId(order.getData().getChannelTransactionId());
        request.setOutOrderNo(order.getBizId());
        // 解冻状态：unfreezeUnsplit 单次分账即解冻剩余金额给商户
        request.setUnfreezeUnsplit(true);

        List<CreateOrderReceiver> receivers = new ArrayList<>();
        CreateOrderReceiver entity = new CreateOrderReceiver();
        entity.setType(ReceiverType.PERSONAL_OPENID.name());
        entity.setAccount(order.getData().getPayeeAccount());
        entity.setAmount((long) order.getData().getShareAmount());
        entity.setDescription("活动收款分账给发起人");
        receivers.add(entity);
        request.setReceivers(receivers);

        try {
            OrdersEntity resp = profitsharingService.createOrder(request);
            return parseShareResult(resp);
        } catch (ServiceException e) {
            log.error("发起分账失败 outOrderNo={}, code={}, msg={}", order.getBizId(), e.getErrorCode(), e.getErrorMessage());
            throw new BusinessException(BizErrorCode.SETTLEMENT_FAILED);
        }
    }

    @Override
    public ChannelShareResult queryProfitShare(String outOrderNo) {
        assertReady();
        QueryOrderRequest request = new QueryOrderRequest();
        request.setOutOrderNo(outOrderNo);
        try {
            OrdersEntity resp = profitsharingService.queryOrder(request);
            return parseShareResult(resp);
        } catch (ServiceException e) {
            log.warn("查询分账失败 outOrderNo={}, code={}", outOrderNo, e.getErrorCode());
            ChannelShareResult result = new ChannelShareResult();
            result.setOutOrderNo(outOrderNo);
            result.setShareState(e.getErrorCode());
            return result;
        }
    }

    private ChannelShareResult parseShareResult(OrdersEntity resp) {
        ChannelShareResult result = new ChannelShareResult();
        result.setOutOrderNo(resp.getOutOrderNo());
        result.setChannelOrderId(resp.getOrderId());
        if (resp.getState() != null) {
            result.setShareState(resp.getState().name());
            if (resp.getState() == OrderStatus.FINISHED) {
                result.setFinished(true);
            }
            // OrderStatus 只有 PROCESSING/FINISHED 两种；FAILED 通过明细 failReason 体现，
            // MVP 不细粒度解析单笔接收方失败，整单失败由 ServiceException 在 profitShare 阶段抛出。
        }
        return result;
    }

    // ==================== 回调验签 + 解密 ====================

    @Override
    public CallbackResult verifyAndParse(String body, Map<String, String> headers) {
        assertReady();
        RequestParam param = new RequestParam.Builder().serialNumber(get(headers, "Wechatpay-Serial")).nonce(get(headers, "Wechatpay-Nonce")).signature(get(headers, "Wechatpay-Signature")).timestamp(get(headers, "Wechatpay-Timestamp")).body(body).build();

        // 支付回调和分账回调走同一个 NotificationParser，按 event_type 区分
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
        } else if (StringUtils.startsWithIgnoreCase(eventType, "PROFITSHARING")) {
            // 分账动账通知（可选加速）：SDK 未提供专门 POJO，解析为 OrdersEntity 兼容字段
            OrdersEntity entity = notificationParser.parse(param, OrdersEntity.class);
            result.setCallbackType("PROFIT_SHARE");
            result.setOutTradeNo(entity.getOutOrderNo());
            result.setChannelTransactionId(entity.getTransactionId());
            result.setSuccess(entity.getState() == OrderStatus.FINISHED);
            result.setDecryptedBody(JSON.toJSONString(entity));
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
        if (jsapiService == null || profitsharingService == null || notificationParser == null) {
            log.error("WechatPayClient SDK 未就绪，配置缺失或初始化失败");
            throw new BusinessException(BizErrorCode.PAYMENT_CHANNEL_NOT_SUPPORTED);
        }
    }

    private String buildDescription(PaymentOrder order) {
        String desc = order.getData().getDescription();
        if (StringUtils.isNotBlank(desc)) {
            return desc;
        }
        return "Rally 约球收款";
    }
}
