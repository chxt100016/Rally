package com.rally.transactionpayment.timeoutorderclose.activity;

import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentChannelRouter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 业务活动 close-expired-payment：关闭渠道未确认付款的到期支付单。
 *
 * <p>本地条件关闭与渠道关单没有统一事务。即使本地条件更新因并发变化而影响零行，
 * 仍会尽力请求渠道关单；渠道异常只记录警告，不恢复本地状态。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CloseExpiredPaymentActivity {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentChannelRouter paymentChannelRouter;

    /**
     * A1-A3：关闭本地待付单、释放活跃业务键，并 best-effort 关闭渠道订单。
     *
     * <p>聚合先在内存中迁移为 CLOSED 并清空 activeRefKey，仓储随后以
     * {@code biz_id + status=PENDING} 条件写入；有意忽略更新结果且不补查。
     * 本活动不读取或修改关联报名，因此报名继续保持 PAYING。</p>
     */
    public PaymentOrder execute(ReconcileExpiredPaymentActivity.Result result) {
        PaymentOrder order = result.paymentOrder();

        // A1-A2：状态与活跃键在同一次条件更新中落库，保留 main 的零行忽略语义。
        order.close();
        paymentOrderRepository.close(order.getBizId());

        // A3：渠道关单失败不影响已经完成的本地关闭，也不向外传播。
        try {
            paymentChannelRouter.route(order.getData().getChannel()).closeTrade(order.getBizId());
        } catch (Exception e) {
            log.warn("渠道关单失败（忽略）: bizId={}, err={}", order.getBizId(), e.getMessage());
        }
        return order;
    }
}
