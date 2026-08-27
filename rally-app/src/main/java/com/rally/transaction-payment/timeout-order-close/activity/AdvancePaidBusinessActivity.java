package com.rally.transactionpayment.timeoutorderclose.activity;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.gateway.PaymentOrderRepository;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.tournament.service.TournamentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 advance-paid-business：确认到期单已付并推进关联赛事业务。
 *
 * <p>本活动故意不建立统一事务。支付单、赛事席位、报名与轮次的写入可分别提交，
 * 任一后续步骤失败均原样传播给调度外层按单记录，不回滚已完成的变化。</p>
 */
@Component
@RequiredArgsConstructor
public class AdvancePaidBusinessActivity {

    private final PaymentOrderRepository paymentOrderRepository;
    private final TournamentPaymentService tournamentPaymentService;

    /**
     * 消费渠道已付结论，先记录本地支付事实，再按业务类型推进关联对象。
     *
     * <p>首次付款结论只取决于扫描时加载的内存状态。聚合使用当前 JVM 的本地时间
     * 写入 payTime；仓储更新仍仅匹配 PENDING，但有意忽略影响行数，不补查、不重试。
     * 因此扫描后并发变更可能使条件更新零行，仍会依据预读 PENDING 继续业务推进。</p>
     *
     * @return 内存中已确认付款的支付单
     */
    public PaymentOrder execute(ReconcileExpiredPaymentActivity.Result result) {
        PaymentOrder order = result.paymentOrder();
        boolean firstPaid = order.isPending();

        // A1：聚合产生本地 payTime；条件更新的结果不改写首次判断。
        order.markPaid(result.channelTransactionId());
        paymentOrderRepository.markPaid(
                order.getBizId(),
                result.channelTransactionId(),
                order.getData().getPayTime());

        // A2-A4：仅预读 PENDING 的报名费单走 main 原有完整推进顺序。
        if (firstPaid && order.getData().getBizType() == BizTypeEnum.TOURNAMENT_ENTRY_FEE) {
            tournamentPaymentService.advanceOnPaid(order);
        }
        return order;
    }
}
