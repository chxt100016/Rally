package com.rally.transactionpayment.paymentresultreceipt.activity;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 confirm-payment：按成功回执确认支付单，并抑制预读已支付的重复推进。
 *
 * <p>状态判断、PENDING 条件更新和首次支付通知沿用既有支付服务，因而保留 main 的
 * 弱并发语义：条件更新的影响行数不改变预读 PENDING 所得到的首次支付结论。</p>
 */
@Component
@RequiredArgsConstructor
public class ConfirmPaymentActivity {

    private final PaymentOrderRepository paymentOrderRepository;
    private final AdvancePaidBusinessActivity advancePaidBusinessActivity;

    /**
     * 按商户单号读取并确认支付。
     *
     * <p>订单不存在、状态非法或持久化失败均原样抛给回执活动；预读 PAID 时既不重复
     * 写入，也不重复通知业务方。PENDING 时由支付聚合写入渠道流水与本地确认时间。</p>
     */
    public PaymentOrder execute(String outTradeNo, String channelTransactionId) {
        PaymentOrder order = paymentOrderRepository.findByBizId(outTradeNo);
        Assert.notNull(order, BizErrorCode.PAYMENT_ORDER_NOT_FOUND);

        boolean firstPaid = order.isPending();
        order.markPaid(channelTransactionId);
        // 保留 main：条件更新只允许 PENDING，但当前不根据影响行数改写首次结论。
        paymentOrderRepository.markPaid(
                outTradeNo, channelTransactionId, order.getData().getPayTime());

        // 只有预读 PENDING 的首次确认才推进业务；预读 PAID 的重复回执直接返回。
        if (firstPaid) {
            advancePaidBusinessActivity.execute(order);
        }
        return order;
    }
}
