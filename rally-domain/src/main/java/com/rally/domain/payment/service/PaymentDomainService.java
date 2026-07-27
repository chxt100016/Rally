package com.rally.domain.payment.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.enums.ChannelEnum;
import com.rally.domain.auth.gateway.AccountRepository;
import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.PaymentLogRepository;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.CallbackResult;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentLog;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.utils.Assert;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 收款单生命周期领域服务（见设计 §15.5）。
 * 薄编排：取聚合根 → 调聚合根行为 / Policy → Repository 持久化 / Client 访问三方。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentDomainService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final PaymentChannelRouter channelRouter;
    private final AccountRepository accountRepository;

    /** @Lazy 打破循环依赖：notifier → handler → TournamentPaymentService → 本服务 */
    @Lazy
    @Resource
    private PaymentPaidNotifier paymentPaidNotifier;

    /**
     * 单人建单（赛事报名费场景）：一单一付款人。refBizId 为业务关联 ID（如 tournamentId）。
     */
    public PaymentOrder createSingle(BizTypeEnum bizType, String refBizId, String payerUserId, int baseAmount, PayChannelEnum channel) {
        // 幂等：已有活跃单（PENDING/PAID）直接返回，历史关闭/失败单不阻塞重建
        PaymentOrder active = paymentOrderRepository.findActiveByRef(bizType, refBizId, payerUserId);
        if (active != null) {
            return active;
        }
        BigDecimal feeRate = SystemConfig.getBigDecimal(SystemConfigKey.PAYMENT_WECHAT_FEE_RATE.getKey());
        int timeoutMinutes = SystemConfig.getInt(SystemConfigKey.PAYMENT_PAY_TIMEOUT_MINUTES.getKey());
        PaymentOrder order = PaymentOrder.create(channel, bizType, refBizId, payerUserId, baseAmount, feeRate, timeoutMinutes);
        try {
            paymentOrderRepository.saveBatch(List.of(order));
        } catch (DuplicateKeyException e) {
            // 并发下另一请求已建活跃单，唯一索引冲突则回查返回既有单（幂等）
            PaymentOrder existing = paymentOrderRepository.findActiveByRef(bizType, refBizId, payerUserId);
            Assert.notNull(existing, BizErrorCode.PAYMENT_ORDER_NOT_FOUND);
            return existing;
        }
        paymentLogRepository.save(PaymentLog.collect(channel, order.getBizId(), "bizType=" + bizType + ",refBizId=" + refBizId + ",base=" + baseAmount));
        return order;
    }

    /**
     * 取拉起参数：load → assertPayer → 渠道下单。参与人点支付时调用。
     */
    public PrepayResult prepay(String outTradeNo, String payerUserId) {
        PaymentOrder order = load(outTradeNo);
        order.assertPayer(payerUserId);
        PaymentChannelClient client = channelRouter.route(order.getData().getChannel());
        // 凭证复用：已有 prepayId 且未过期，直接重签拉起参数，不重新下单
        if (order.isPrepayReusable()) {
            return client.buildRequestPayment(order.getData().getPrepayId());
        }
        // 反查付款人在该渠道的身份标识（微信小程序 openid），JSAPI 下单必需
        String payerOpenid = accountRepository.findIdentifierByUser(payerUserId, ChannelEnum.WECHAT_MINIAPP);
        Assert.notBlank(payerOpenid, BizErrorCode.WECHAT_AUTH_FAILED);
        PrepayResult result = client.prepay(order, payerOpenid);
        // 回写 prepayId + 有效期（微信 prepay_id 有效期 2 小时，不超过支付单本身超时）
        LocalDateTime prepayExpire = order.calcPrepayExpireTime();
        paymentOrderRepository.updatePrepay(outTradeNo, result.getPrepayId(), prepayExpire);
        paymentLogRepository.save(PaymentLog.prepay(order.getData().getChannel(), outTradeNo, "openid=" + payerOpenid));
        return result;
    }

    /**
     * 回调/查单推进：load → markPaid（条件更新落库）。返回推进后的单。幂等。
     */
    /**
     * 支付异步回调处理：验签解密 + 留痕 + 推进状态（业务分流由 markPaid 内 notifier 完成）。
     * @return true 处理成功；false 需告知渠道重试
     */
    @Transactional
    public boolean handleCallback(String body, Map<String, String> headers) {
        PaymentChannelClient client = channelRouter.route(PayChannelEnum.WECHAT);
        CallbackResult callback;
        try {
            callback = client.verifyAndParse(body, headers);
        } catch (Exception e) {
            log.error("支付回调验签解密失败", e);
            return false;
        }
        // 留痕（落 RECEIVED，处理成功后转 PROCESSED）
        PaymentLog logEntry = PaymentLog.callback(PayChannelEnum.WECHAT, "ORDER", callback.getOutTradeNo(), callback.getDecryptedBody());
        paymentLogRepository.save(logEntry);
        try {
            if ("TRANSACTION".equals(callback.getCallbackType()) && callback.isSuccess() && StringUtils.isNotBlank(callback.getOutTradeNo())) {
                markPaid(callback.getOutTradeNo(), callback.getChannelTransactionId());
            }
            logEntry.markProcessed();
            paymentLogRepository.update(logEntry);
            return true;
        } catch (Exception e) {
            log.error("支付回调处理失败 outTradeNo={}", callback.getOutTradeNo(), e);
            logEntry.markFailed(e.getMessage());
            paymentLogRepository.update(logEntry);
            return false;
        }
    }

    public PaymentOrder markPaid(String outTradeNo, String transactionId) {
        PaymentOrder order = load(outTradeNo);
        boolean firstPaid = order.isPending();
        order.markPaid(transactionId);
        paymentOrderRepository.markPaid(outTradeNo, transactionId, order.getData().getPayTime());
        // 仅首次由 PENDING→PAID 时通知业务方，重复回调幂等不重复通知
        if (firstPaid) {
            paymentPaidNotifier.notifyPaid(order);
        }
        return order;
    }

    /**
     * 超时关单（PaymentTimeoutJob 用）：queryTrade 已付则补 markPaid，否则 close()+closeTrade。
     */
    public PaymentOrder timeoutCheck(PaymentOrder order) {
        PaymentChannelClient client = channelRouter.route(order.getData().getChannel());
        ChannelTradeResult trade = client.queryTrade(order.getBizId());
        if (trade != null && trade.isPaid()) {
            return markPaid(order.getBizId(), trade.getChannelTransactionId());
        }
        order.close();
        paymentOrderRepository.close(order.getBizId());
        closeTradeQuietly(order);
        return order;
    }

    /**
     * 回调补偿用（PaymentCallbackRecoverJob）：仅查单，已支付补 markPaid 并返回；未支付不关单。
     */
    public PaymentOrder recoverIfPaid(PaymentOrder order) {
        PaymentChannelClient client = channelRouter.route(order.getData().getChannel());
        ChannelTradeResult trade = client.queryTrade(order.getBizId());
        if (trade != null && trade.isPaid()) {
            return markPaid(order.getBizId(), trade.getChannelTransactionId());
        }
        return order;
    }

    private void closeTradeQuietly(PaymentOrder order) {
        try {
            channelRouter.route(order.getData().getChannel()).closeTrade(order.getBizId());
        } catch (Exception e) {
            log.warn("渠道关单失败（忽略）: bizId={}, err={}", order.getBizId(), e.getMessage());
        }
    }

    public PaymentOrder load(String outTradeNo) {
        PaymentOrder order = paymentOrderRepository.findByBizId(outTradeNo);
        Assert.notNull(order, BizErrorCode.PAYMENT_ORDER_NOT_FOUND);
        return order;
    }
}
