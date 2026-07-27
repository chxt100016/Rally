package com.rally.domain.tournament.service;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentPaidHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 赛事报名费支付成功处理器：推进 entry 状态 + 锁定正赛席位（能力四策略实现）。
 */
@Component
@RequiredArgsConstructor
public class TournamentEntryPaidHandler implements PaymentPaidHandler {

    private final TournamentPaymentService tournamentPaymentService;

    @Override
    public boolean supports(BizTypeEnum bizType) {
        return bizType == BizTypeEnum.TOURNAMENT_ENTRY_FEE;
    }

    @Override
    public void onPaid(PaymentOrder paidOrder) {
        tournamentPaymentService.advanceOnPaid(paidOrder);
    }
}
