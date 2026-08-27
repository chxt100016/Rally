package com.rally.transactionpayment.receiptrecovery.activity;

import com.rally.domain.payment.gateway.PaymentChannelClient;
import com.rally.domain.payment.gateway.PaymentLogRepository;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.ChannelTradeResult;
import com.rally.domain.payment.model.PaymentLog;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentChannelRouter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务活动 reconcile-payment-status：扫描滞留支付回执并逐条复核关联支付单。
 *
 * <p>扫描、单条复核和支付确认都不建立统一事务。调用方应逐条消费
 * {@link Result}，并在本活动返回后分别推进已付业务和结束回执留痕。</p>
 */
@Component
@RequiredArgsConstructor
public class ReconcilePaymentStatusActivity {

    private static final int RECEIVED_TIMEOUT_MINUTES = 5;
    private static final String PAYMENT_ORDER_REFERENCE = "ORDER";
    private static final String PAYMENT_ORDER_NOT_FOUND = "payment_order_not_found";

    private final PaymentLogRepository paymentLogRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentChannelRouter paymentChannelRouter;

    /**
     * A1：一次性读取五分钟前仍为 CALLBACK/RECEIVED 的全部回执。
     *
     * <p>沿用现有仓储查询，不增加分页、数量限制或排序。</p>
     */
    public List<PaymentLog> scan() {
        LocalDateTime before = LocalDateTime.now().minusMinutes(RECEIVED_TIMEOUT_MINUTES);
        return paymentLogRepository.listUnprocessedCallback(before);
    }

    /**
     * 复核一条扫描所得回执；渠道或持久化异常原样上抛，由外层逐条失败留痕。
     */
    public Result execute(PaymentLog receipt) {
        String refType = receipt.getData().getRefType();
        String refId = receipt.getData().getRefId();

        // A1/A2：只有精确 ORDER 且 refId 非空白的回执才读取支付单。
        if (!PAYMENT_ORDER_REFERENCE.equals(refType) || StringUtils.isBlank(refId)) {
            return Result.processed(receipt, null);
        }

        PaymentOrder order = paymentOrderRepository.findByBizId(refId);
        if (order == null) {
            return Result.failed(receipt, PAYMENT_ORDER_NOT_FOUND);
        }

        // A2：本地终态不查渠道，也不补做此前可能遗漏的业务推进。
        if (!order.isPending()) {
            return Result.processed(receipt, order);
        }

        // A3：微信客户端把 ServiceException 转换为 paid=false；其余路由、配置或
        // 客户端异常继续外抛。未付时保持 PENDING，之后回执仍由后续活动结束。
        PaymentChannelClient client = paymentChannelRouter.route(order.getData().getChannel());
        ChannelTradeResult trade = client.queryTrade(order.getBizId());
        if (trade == null || !trade.isPaid()) {
            return Result.processed(receipt, order);
        }

        // A4：本地确认时间由聚合在当前 JVM 生成。首次结论只取加载时 PENDING；
        // 条件更新影响行数有意忽略，不补查、不重试，保留 main 的弱并发语义。
        boolean firstPaid = order.isPending();
        order.markPaid(trade.getChannelTransactionId());
        paymentOrderRepository.markPaid(
                order.getBizId(),
                trade.getChannelTransactionId(),
                order.getData().getPayTime());
        return new Result(receipt, order, firstPaid, null);
    }

    /** 单条复核结论；失败原因非空时由后续活动把回执结束为 FAILED。 */
    public record Result(
            PaymentLog receipt,
            PaymentOrder paymentOrder,
            boolean firstPaid,
            String failureReason) {

        public static Result processed(PaymentLog receipt, PaymentOrder paymentOrder) {
            return new Result(receipt, paymentOrder, false, null);
        }

        public static Result failed(PaymentLog receipt, String failureReason) {
            return new Result(receipt, null, false, failureReason);
        }

        public boolean failed() {
            return failureReason != null;
        }
    }
}
