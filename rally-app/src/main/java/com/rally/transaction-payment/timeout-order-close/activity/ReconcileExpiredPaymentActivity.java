package com.rally.transactionpayment.timeoutorderclose.activity;

import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentChannelRouter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务活动 reconcile-expired-payment：扫描到期待付单并逐笔核实渠道付款结果。
 *
 * <p>扫描、单笔查单与后续已付/未付处理不共享统一事务。调用方负责逐笔
 * 捕获未转换异常，并把 {@link Result} 分流给业务推进或超时关单活动。</p>
 */
@Component
@RequiredArgsConstructor
public class ReconcileExpiredPaymentActivity {

    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentChannelRouter paymentChannelRouter;

    /**
     * A1：以本次调用的时刻一次性扫描全部严格到期的 PENDING 支付单。
     *
     * <p>现有仓储保持 {@code expire_time IS NOT NULL AND expire_time < now}，
     * 不分页、不限量且不增加排序。</p>
     */
    public List<PaymentOrder> scan() {
        return paymentOrderRepository.listExpiredPending(LocalDateTime.now());
    }

    /**
     * A2-A4：按订单渠道路由并查单，返回已付或待关闭结论。
     *
     * <p>微信客户端会将 SDK {@code ServiceException} 转换成 {@code paid=false}
     * 的查单结果；不支持渠道、SDK 未就绪或其他未转换异常原样外抛。
     * 不重新加载或复核本地状态，保留扫描后并发变化的弱语义。</p>
     */
    public Result execute(PaymentOrder order) {
        PaymentChannelClient client = paymentChannelRouter.route(order.getData().getChannel());
        ChannelTradeResult trade = client.queryTrade(order.getBizId());
        if (trade != null && trade.isPaid()) {
            return Result.paid(order, trade.getChannelTransactionId());
        }
        return Result.unpaid(order);
    }

    /** 单笔渠道核实结论；非成功状态与可转换查单错误均表示待关闭。 */
    public record Result(PaymentOrder paymentOrder, boolean paid, String channelTransactionId) {

        public static Result paid(PaymentOrder paymentOrder, String channelTransactionId) {
            return new Result(paymentOrder, true, channelTransactionId);
        }

        public static Result unpaid(PaymentOrder paymentOrder) {
            return new Result(paymentOrder, false, null);
        }
    }
}
