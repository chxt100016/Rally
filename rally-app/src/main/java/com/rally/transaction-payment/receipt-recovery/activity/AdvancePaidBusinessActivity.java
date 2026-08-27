package com.rally.transactionpayment.receiptrecovery.activity;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.tournament.service.TournamentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 advance-paid-business：为恢复任务本次确认的报名费付款推进赛事业务。
 */
@Component
@RequiredArgsConstructor
public class AdvancePaidBusinessActivity {

    private final TournamentPaymentService tournamentPaymentService;

    /**
     * 仅处理本次从 PENDING 首次确认为 PAID 的赛事报名费。
     *
     * <p>本活动故意不建立统一事务，也不捕获领域异常。领域服务依次读取报名与赛事、
     * 条件锁定正赛席位、将 PAYING 报名推进为 MAIN/WAITING/首轮，
     * 然后评估轮次并在满位时淘汰剩余资格等待报名。任一步失败都原样传给外层，
     * 由 finalize 活动把当前回执标为失败，且保留异常前已提交的 PAID 或席位变化。</p>
     *
     * @return 原复核结果，便于后续结束回执留痕
     */
    public ReconcilePaymentStatusActivity.Result execute(
            ReconcilePaymentStatusActivity.Result result) {
        if (!result.firstPaid()) {
            return result;
        }

        PaymentOrder paidOrder = result.paymentOrder();
        if (paidOrder.getData().getBizType() != BizTypeEnum.TOURNAMENT_ENTRY_FEE) {
            return result;
        }

        // A1-A4：读取报名赛事、原子占位、推进报名、评估轮次均沿用主线顺序。
        tournamentPaymentService.advanceOnPaid(paidOrder);
        return result;
    }
}
