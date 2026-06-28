package com.rally.domain.payment.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.PaymentLogRepository;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentLog;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private final PaymentCollectionPolicy collectionPolicy;

    /**
     * 发起收款：校验 + 平摊 → 逐个建单（PENDING）→ 批量落库。应付人名单已在 app 层取约球只读名单后传入。
     */
    public List<PaymentOrder> createBatch(String meetupId, String payeeUserId, String payeeAccount, List<String> payerUserIds, int totalAmount, PayChannelEnum channel) {
        collectionPolicy.assertCollect(totalAmount, payerUserIds);
        // MVP 每场仅一次发起，本场已存在支付单则拒绝
        Assert.isTrue(!paymentOrderRepository.existsByMeetup(meetupId), BizErrorCode.COLLECTION_NOT_ALLOWED);

        int[] amounts = collectionPolicy.amortize(totalAmount, payerUserIds.size());
        BigDecimal feeRate = SystemConfig.getBigDecimal(SystemConfigKey.PAYMENT_WECHAT_FEE_RATE.getKey());
        int timeoutMinutes = SystemConfig.getInt(SystemConfigKey.PAYMENT_PAY_TIMEOUT_MINUTES.getKey());
        String batchId = IdWorker.getIdStr();

        List<PaymentOrder> orders = new ArrayList<>(payerUserIds.size());
        for (int i = 0; i < payerUserIds.size(); i++) {
            orders.add(PaymentOrder.create(channel, batchId, meetupId, payerUserIds.get(i), payeeUserId, payeeAccount, amounts[i], feeRate, timeoutMinutes));
        }
        paymentOrderRepository.saveBatch(orders);
        orders.forEach(o -> paymentLogRepository.save(PaymentLog.collect(channel, o.getBizId(), "batch=" + batchId + ",base=" + o.getData().getBaseAmount())));
        return orders;
    }

    /**
     * 取拉起参数：load → assertPayer → 渠道下单。参与人点支付时调用。
     */
    public PrepayResult prepay(String outTradeNo, String payerUserId, String payerOpenid) {
        PaymentOrder order = load(outTradeNo);
        order.assertPayer(payerUserId);
        PaymentChannelClient client = channelRouter.route(order.getData().getChannel());
        PrepayResult result = client.prepay(order, payerOpenid);
        paymentLogRepository.save(PaymentLog.prepay(order.getData().getChannel(), outTradeNo, "openid=" + payerOpenid));
        return result;
    }

    /**
     * 回调/查单推进：load → markPaid（条件更新落库）。返回推进后的单用于触发分账。幂等。
     */
    public PaymentOrder markPaid(String outTradeNo, String transactionId) {
        PaymentOrder order = load(outTradeNo);
        order.markPaid(transactionId);
        paymentOrderRepository.markPaid(outTradeNo, transactionId, order.getData().getPayTime());
        return order;
    }

    /**
     * 关闭收款：本场全部 PENDING → 逐个 close() + 渠道 closeTrade（best-effort）；已 PAID 不动（§5.4.2）。
     */
    public void closeBatch(String meetupId, String operatorUserId) {
        List<PaymentOrder> pendingOrders = paymentOrderRepository.listPendingByMeetup(meetupId);
        for (PaymentOrder order : pendingOrders) {
            order.close();
            paymentOrderRepository.close(order.getBizId());
            closeTradeQuietly(order);
        }
        log.info("关闭收款: meetupId={}, operator={}, closed={}", meetupId, operatorUserId, pendingOrders.size());
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

    private PaymentOrder load(String outTradeNo) {
        PaymentOrder order = paymentOrderRepository.findByBizId(outTradeNo);
        Assert.notNull(order, BizErrorCode.PAYMENT_ORDER_NOT_FOUND);
        return order;
    }
}
