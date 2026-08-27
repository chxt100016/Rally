package com.rally.transactionpayment.paymentstatussync.activity;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.tournament.service.TournamentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 advance-paid-business：为同步查单首次确认的报名费付款推进赛事业务。
 */
@Component
@RequiredArgsConstructor
public class AdvancePaidBusinessActivity {

    private final TournamentPaymentService tournamentPaymentService;

    /**
     * 只处理本次由 PENDING 首次确认为 PAID 的赛事报名费。
     *
     * <p>调用方应在包含支付确认的同一应用事务中调用本方法。
     * 本方法不捕获报名、赛事、席位或轮次异常，使它们向外传播并回滚本次
     * 支付确认及所有本地业务变更。已 PAID 的同步结果会按
     * {@link ReconcilePaymentStatusActivity.Result#firstPaid()} 短路，保留现有补偿缺口。</p>
     *
     * @return 原同步结果，便于后续交付支付摘要
     */
    @Transactional(rollbackFor = Exception.class)
    public ReconcilePaymentStatusActivity.Result execute(
            ReconcilePaymentStatusActivity.Result result) {
        if (!result.firstPaid()) {
            return result;
        }

        PaymentOrder paidOrder = result.paymentOrder();
        if (paidOrder.getData().getBizType() != BizTypeEnum.TOURNAMENT_ENTRY_FEE) {
            return result;
        }

        // A1-A4：沿用 main 的完整推进顺序和错误语义，不在活动层改写或吞掉异常。
        tournamentPaymentService.advanceOnPaid(paidOrder);
        return result;
    }
}
