package com.rally.tournament.entrypaymentinitiate.activity;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayDTO;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryPayCmd;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.tournament.service.TournamentPaymentService;
import com.rally.payment.convert.PaymentAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 业务活动 prepare-entry-payment：建立或复用报名费支付单并交付微信付款参数。 */
@Component
@RequiredArgsConstructor
public class PrepareEntryPaymentActivity {

    private final TournamentAdminService tournamentAdminService;
    private final TournamentEntryService tournamentEntryService;
    private final TournamentPaymentService tournamentPaymentService;
    private final PaymentDomainService paymentDomainService;

    /**
     * 保持既有报名支付入口的本地事务边界；微信侧下单和关单不参与本地事务。
     */
    @Transactional
    public PrepayDTO execute(TournamentEntryPayCmd cmd) {
        String payerUserId = UserContext.get();

        // A1：赛事、本人报名、PAYING 状态和剩余正赛席位均由既有领域入口校验。
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(
                cmd.getTournamentId(), payerUserId);

        /*
         * A2：按报名业务号、付款人和赛事报名费建立或复用微信活跃单。
         * 领域服务保持原有超时单关闭、新建、活跃键并发收敛和金额计算语义。
         */
        PaymentOrder order = tournamentPaymentService.createEntryOrder(entry, tournament);

        /*
         * A3-A5：prepay 内部再次校验 payer，解析微信身份和渠道；满足既有复用
         * 条件时按 prepayId 重签，否则调用微信下单并按业务号保存渠道返回的
         * prepayId/有效期。活动不附加状态、凭证边界或更新行数门禁，只交付拉起
         * 参数，不推进报名或支付状态。
         */
        PrepayResult result = paymentDomainService.prepay(order.getBizId(), payerUserId);
        return PaymentAppConvertMapper.INSTANCE.toPrepayDTO(result, order.getBizId());
    }
}
