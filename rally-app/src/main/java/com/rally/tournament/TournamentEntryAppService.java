package com.rally.tournament;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayCmd;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.tournament.model.TournamentEntryPayCmd;
import com.rally.domain.tournament.model.TournamentEntryUpdateCmd;
import com.rally.domain.tournament.model.TournamentJoinCmd;
import com.rally.domain.tournament.model.TournamentWithdrawCmd;
import com.rally.domain.tournament.model.TournamentWithdrawResultDTO;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.tournament.service.TournamentPaymentService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.domain.utils.Assert;
import com.rally.payment.PaymentAppService;
import com.rally.tournament.convert.TournamentEntryAppConvertMapper;
import com.rally.utils.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 报名（用户端）写流程编排：报名/修改偏好/退出
 */
@Service
@RequiredArgsConstructor
public class TournamentEntryAppService {

    private final TournamentAdminService tournamentAdminService;
    private final TournamentEntryService tournamentEntryService;
    private final UserProfileDomainService userProfileDomainService;
    private final TournamentPaymentService tournamentPaymentService;
    private final PaymentAppService paymentAppService;

    /**
     * 报名
     */
    @Transactional
    public TournamentEntryDTO join(TournamentJoinCmd cmd) {
        String userId = UserContext.get();
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());
        UserProfile userProfile = userProfileDomainService.get(userId);

        TournamentEntry entry = tournamentEntryService.join(tournament, userProfile, userId, cmd);
        return TournamentEntryAppConvertMapper.INSTANCE.toTournamentEntryDTO(entry.getData());
    }

    /**
     * 修改报名偏好，仅本人、仅排队态可改
     */
    @Transactional
    public void update(TournamentEntryUpdateCmd cmd) {
        String userId = UserContext.get();
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);
        tournamentEntryService.updatePreference(entry, cmd);
    }

    /**
     * 支付报名费：校验 entry 为 PAYING 且赛事未满 → 建单 → 取拉起参数
     */
    @Transactional
    public PrepayResult pay(TournamentEntryPayCmd cmd) {
        String userId = UserContext.get();
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);

        PaymentOrder order = tournamentPaymentService.createEntryOrder(entry, tournament);
        PrepayCmd prepayCmd = new PrepayCmd();
        prepayCmd.setPaymentId(order.getBizId());
        return paymentAppService.prepay(prepayCmd);
    }

    /**
     * 退出赛事：资格赛阶段直接退出，正赛阶段（已支付）需先退款（暂未开放）
     */
    @Transactional
    public TournamentWithdrawResultDTO withdraw(TournamentWithdrawCmd cmd) {
        String userId = UserContext.get();
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);

        if (entry.isMainStage()) {
            // 正赛阶段退款流程属模块 5（支付与晋级），MVP 首版暂未开放
            Assert.isTrue(false, BizErrorCode.TOURNAMENT_REFUND_NOT_SUPPORTED);
        }

        tournamentEntryService.withdrawQualify(entry);
        return new TournamentWithdrawResultDTO(false);
    }
}
