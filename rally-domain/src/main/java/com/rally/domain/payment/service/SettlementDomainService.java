package com.rally.domain.payment.service;

import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.SettlementOrderRepository;
import com.rally.domain.payment.model.ChannelShareResult;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.SettlementOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 分账生命周期领域服务（见设计 §15.5、§5.3）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementDomainService {

    private final SettlementOrderRepository settlementOrderRepository;
    private final PaymentChannelRouter channelRouter;

    /**
     * 收款成功后分账（可同步或转 Job）：assertPaid + 幂等(uk_payment_order 已存在直接返回)
     * → SettlementOrder.createFrom → channel.profitShare → markProcessing 落库。
     */
    public SettlementOrder share(PaymentOrder paidOrder) {
        paidOrder.assertPaid();
        SettlementOrder existing = settlementOrderRepository.findByPaymentOrderId(paidOrder.getBizId());
        if (existing != null) {
            return existing;
        }
        SettlementOrder order = SettlementOrder.createFrom(paidOrder);
        settlementOrderRepository.save(order);

        PaymentChannelClient client = channelRouter.route(order.getData().getChannel());
        ChannelShareResult result = client.profitShare(order);
        order.markProcessing(result == null ? null : result.getChannelOrderId());
        settlementOrderRepository.markProcessing(order.getBizId(), order.getData().getChannelOrderId());
        return order;
    }

    /**
     * 对账推进（SettlementReconcileJob 用）：channel.queryProfitShare → markFinished/markFailed 落库。
     */
    public void reconcile(SettlementOrder order) {
        if (!order.isProcessing()) {
            return;
        }
        ChannelShareResult result = channelRouter.route(order.getData().getChannel()).queryProfitShare(order.getBizId());
        if (result == null) {
            return;
        }
        if (result.isFinished()) {
            order.markFinished();
            settlementOrderRepository.markFinished(order.getBizId(), LocalDateTime.now());
        } else if (result.isFailed()) {
            order.markFailed(result.getFailReason());
            settlementOrderRepository.markFailed(order.getBizId(), result.getFailReason());
        }
    }
}
