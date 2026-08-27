package com.rally.transactionpayment.paymentstatussync.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentChannelRouter;
import com.rally.domain.utils.Assert;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 reconcile-payment-status：校验付款人并按需以渠道结果确认支付单。
 */
@Component
@RequiredArgsConstructor
public class ReconcilePaymentStatusActivity {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentChannelRouter paymentChannelRouter;

    /**
     * 返回本次加载并可能确认后的支付单，以及是否按加载时状态判定为首次支付。
     *
     * <p>事务参与方式沿用同步支付入口：后续业务推进由调用方根据
     * {@link Result#firstPaid()} 在同一外层事务内继续执行，任一异常向外传播时
     * 回滚本次本地确认；渠道侧付款事实不受本地事务影响。</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public Result execute(String paymentId) {
        String payerUserId = UserContext.get();

        // A1：不存在与非付款人分别保持既有稳定错误码。
        PaymentOrder order = paymentOrderRepository.findByBizId(paymentId);
        Assert.notNull(order, BizErrorCode.PAYMENT_ORDER_NOT_FOUND);
        order.assertPayer(payerUserId);

        // A2：PAID/CLOSED/FAILED 均不再访问渠道，也不重复推进业务。
        if (!order.isPending()) {
            return new Result(order, false);
        }

        // A3：渠道或 SDK 不可用时路由/客户端异常原样外抛；微信 ServiceException
        // 已由现有客户端转换为 paid=false 的查单结果，因而与普通未付相同短路。
        PaymentChannelClient client = paymentChannelRouter.route(order.getData().getChannel());
        ChannelTradeResult trade = client.queryTrade(order.getBizId());
        if (trade == null || !trade.isPaid()) {
            return new Result(order, false);
        }

        // A4：首次结论只取决于更新前已加载的 PENDING 状态。聚合写入本地确认时间，
        // 仓储仍以 status=PENDING 条件更新，但有意忽略影响行数，不补查、不重试。
        boolean firstPaid = order.isPending();
        order.markPaid(trade.getChannelTransactionId());
        paymentOrderRepository.markPaid(
                order.getBizId(),
                trade.getChannelTransactionId(),
                order.getData().getPayTime());
        return new Result(order, firstPaid);
    }

    /** 同步确认结果；后续活动只在 {@code firstPaid=true} 时推进关联业务。 */
    public record Result(PaymentOrder paymentOrder, boolean firstPaid) {
    }
}
