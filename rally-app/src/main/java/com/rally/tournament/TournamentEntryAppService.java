package com.rally.tournament;

import com.rally.domain.payment.model.PaymentOrder;
import com.rally.domain.payment.model.PrepayDTO;
import com.rally.domain.payment.model.PrepayResult;
import com.rally.domain.payment.service.PaymentDomainService;
import com.rally.domain.notify.enums.NotifyBizType;
import com.rally.domain.notify.service.NotifySubscribeService;
import com.rally.domain.meetup.service.ChatDomainService;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.tournament.model.TournamentEntryPayCmd;
import com.rally.domain.tournament.model.TournamentEntryUnfreezeCmd;
import com.rally.domain.tournament.model.TournamentEntryUpdateCmd;
import com.rally.domain.tournament.model.TournamentJoinCmd;
import com.rally.domain.tournament.model.TournamentWithdrawCmd;
import com.rally.domain.tournament.model.TournamentWithdrawResultDTO;
import com.rally.domain.tournament.service.TournamentAdminService;
import com.rally.domain.tournament.service.TournamentEntryService;
import com.rally.domain.tournament.service.TournamentMatchFlowService;
import com.rally.domain.tournament.service.TournamentPaymentService;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.payment.convert.PaymentAppConvertMapper;
import com.rally.notify.TournamentNotifyAssembler;
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
    private final PaymentDomainService paymentDomainService;
    private final TournamentMatchFlowService tournamentMatchFlowService;
    private final NotifySubscribeService notifySubscribeService;
    private final ChatDomainService chatDomainService;

    /**
     * 报名
     */
    @Transactional
    public TournamentEntryDTO join(TournamentJoinCmd cmd) {
        String userId = UserContext.get();
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());
        UserProfile userProfile = userProfileDomainService.get(userId);
        userProfile.assertCompleted();

        TournamentEntry entry = tournamentEntryService.join(tournament, userProfile, userId, cmd);
        chatDomainService.join(cmd.getTournamentId(), userId);
        notifySubscribeService.grant(userId, NotifyBizType.TOURNAMENT, cmd.getTournamentId(),
                TournamentNotifyAssembler.parseScenes(cmd.getAcceptedNoticeScenes()));
        return TournamentEntryAppConvertMapper.INSTANCE.toTournamentEntryDTO(entry.getData());
    }

    /**
     * 修改报名偏好，仅本人、仅排队态或待支付态可改
     */
    @Transactional
    public void update(TournamentEntryUpdateCmd cmd) {
        String userId = UserContext.get();
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);
        tournamentEntryService.updatePreference(entry, cmd);
    }

    /** 解冻本人报名，恢复等待匹配状态。 */
    @Transactional
    public void unfreeze(TournamentEntryUnfreezeCmd cmd) {
        String userId = UserContext.get();
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());
        UserProfile userProfile = userProfileDomainService.get(userId);
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);
        tournamentEntryService.unfreeze(tournament, entry, userProfile);
    }

    /**
     * 支付报名费：校验 entry 为 PAYING 且赛事未满 → 建单 → 取拉起参数
     */
    @Transactional
    public PrepayDTO pay(TournamentEntryPayCmd cmd) {
        String userId = UserContext.get();
        Tournament tournament = tournamentAdminService.get(cmd.getTournamentId());
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);

        PaymentOrder order = tournamentPaymentService.createEntryOrder(entry, tournament);
        PrepayResult result = paymentDomainService.prepay(order.getBizId(), userId);
        return PaymentAppConvertMapper.INSTANCE.toPrepayDTO(result, order.getBizId());
    }

    /**
     * 退出赛事：置 WITHDRAWN（资格赛/正赛通用）；若正在比赛中，关闭比赛并让对手回匹配池
     */
    @Transactional
    public TournamentWithdrawResultDTO withdraw(TournamentWithdrawCmd cmd) {
        String userId = UserContext.get();
        TournamentEntry entry = tournamentEntryService.getByTournamentAndUser(cmd.getTournamentId(), userId);

        tournamentEntryService.withdraw(entry);
        chatDomainService.quit(cmd.getTournamentId(), userId);
        tournamentMatchFlowService.closeActiveMatchOnWithdraw(cmd.getTournamentId(), userId);
        return new TournamentWithdrawResultDTO(false);
    }
}
