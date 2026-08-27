package com.rally.transactionpayment.paymentresultreceipt.activity;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.tournament.service.TournamentPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 业务活动 advance-paid-business：推进首次付款对应的赛事报名、席位与轮次。
 */
@Component
@RequiredArgsConstructor
public class AdvancePaidBusinessActivity {

    private final TournamentPaymentService tournamentPaymentService;

    /**
     * 调用方只在预读 PENDING 的订单首次确认为 PAID 后进入；这里按业务类型分流。
     *
     * <p>赛事推进继续复用 main 的顺序：读取报名赛事、条件占位、要求报名 PAYING，
     * 写 MAIN/WAITING/paidTime/正赛首轮，再评估赛事轮次并在满位时淘汰剩余
     * QUALIFY/WAITING 报名。异常原样传播，由回执活动内部捕获，从而保留既有的
     * 部分提交边界和错误语义。</p>
     */
    public void execute(PaymentOrder paidOrder) {
        if (paidOrder.getData().getBizType() != BizTypeEnum.TOURNAMENT_ENTRY_FEE) {
            return;
        }
        tournamentPaymentService.advanceOnPaid(paidOrder);
    }
}
