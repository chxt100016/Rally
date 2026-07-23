package com.rally.domain.tournament.service;

import com.rally.domain.payment.enums.BizTypeEnum;
import com.rally.domain.payment.enums.PayChannelEnum;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.utils.Assert;
import com.rally.domain.auth.enums.BizErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 赛事支付与晋级领域服务（编排 Payment 域，见模块 5）：资格赛胜者支付锁正赛席位，推进 entry/tournament 状态。
 * 报名费归平台所有，不走分账，payeeUserId/payeeAccount 留空。
 */
@Service
@RequiredArgsConstructor
public class TournamentPaymentService {

    private final PaymentDomainService paymentDomainService;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository tournamentEntryRepository;

    /**
     * 建单：校验 entry 处于 PAYING、赛事未满员 → 建单（payeeUserId/payeeAccount 为空，报名费归平台不分账）
     */
    public PaymentOrder createEntryOrder(TournamentEntry entry, Tournament tournament) {
        entry.assertCanPay();
        tournament.assertSlotsNotFull();
        int baseAmount = tournament.getData().getEntryFee().intValue();
        return paymentDomainService.createSingle(BizTypeEnum.TOURNAMENT_ENTRY_FEE, tournament.getTournamentId(), entry.getData().getUserId(), null, null, baseAmount, PayChannelEnum.WECHAT);
    }

    /**
     * 支付成功推进：entry PAYING→WAITING、stage→MAIN，写 paidTime；席位原子 +1（防超卖，失败则整体回滚由外层事务保证）。
     * bizRefId 即建单时冗余的 tournamentId（存于 payment_order.meetup_id 列）。
     */
    public void advanceOnPaid(PaymentOrder paidOrder) {
        String tournamentId = paidOrder.getData().getMeetupId();
        String userId = paidOrder.getData().getPayerUserId();

        Tournament tournament = getTournament(tournamentId);
        TournamentEntry entry = getEntry(tournamentId, userId);

        boolean locked = tournamentRepository.incrementFilledSlots(tournamentId);
        Assert.isTrue(locked, BizErrorCode.TOURNAMENT_SLOTS_FULL);

        entry.advanceToMainDrawPaid(tournament.getData().getTotalSlots());
        tournamentEntryRepository.save(entry.getData());
    }

    /**
     * 兜底主动查单：复用 PaymentDomainService.recoverIfPaid，已支付则回调同一条推进逻辑
     */
    public void queryAndAdvance(PaymentOrder order) {
        PaymentOrder recovered = paymentDomainService.recoverIfPaid(order);
        if (recovered.isPending()) {
            return;
        }
        advanceOnPaid(recovered);
    }

    private Tournament getTournament(String tournamentId) {
        var data = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(data, BizErrorCode.TOURNAMENT_NOT_FOUND);
        return new Tournament(data);
    }

    private TournamentEntry getEntry(String tournamentId, String userId) {
        var data = tournamentEntryRepository.findByTournamentAndUser(tournamentId, userId);
        Assert.notNull(data, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(data);
    }
}
