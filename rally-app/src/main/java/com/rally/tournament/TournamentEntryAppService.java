package com.rally.tournament;

import com.rally.domain.payment.model.PrepayDTO;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryDTO;
import com.rally.domain.tournament.model.TournamentEntryPayCmd;
import com.rally.domain.tournament.model.TournamentEntryUnfreezeCmd;
import com.rally.domain.tournament.model.TournamentEntryUpdateCmd;
import com.rally.domain.tournament.model.TournamentJoinCmd;
import com.rally.domain.tournament.model.TournamentWithdrawCmd;
import com.rally.domain.tournament.model.TournamentWithdrawResultDTO;
import com.rally.tournament.convert.TournamentEntryAppConvertMapper;
import com.rally.tournament.entrypreferenceupdate.activity.ReplaceEntryPreferenceActivity;
import com.rally.tournament.entrypaymentinitiate.activity.PrepareEntryPaymentActivity;
import com.rally.tournament.entryunfreeze.activity.UnfreezeEntryActivity;
import com.rally.tournament.tournamententry.activity.JoinTournamentDiscussionActivity;
import com.rally.tournament.tournamententry.activity.RegisterTournamentEntryActivity;
import com.rally.tournament.tournamentwithdraw.activity.LeaveTournamentDiscussionActivity;
import com.rally.tournament.tournamentwithdraw.activity.TerminateWithdrawnMatchActivity;
import com.rally.tournament.tournamentwithdraw.activity.WithdrawTournamentEntryActivity;
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

    private final RegisterTournamentEntryActivity registerTournamentEntryActivity;
    private final JoinTournamentDiscussionActivity joinTournamentDiscussionActivity;
    private final ReplaceEntryPreferenceActivity replaceEntryPreferenceActivity;
    private final PrepareEntryPaymentActivity prepareEntryPaymentActivity;
    private final UnfreezeEntryActivity unfreezeEntryActivity;
    private final WithdrawTournamentEntryActivity withdrawTournamentEntryActivity;
    private final LeaveTournamentDiscussionActivity leaveTournamentDiscussionActivity;
    private final TerminateWithdrawnMatchActivity terminateWithdrawnMatchActivity;

    /**
     * 报名
     */
    @Transactional
    public TournamentEntryDTO join(TournamentJoinCmd cmd) {
        String userId = UserContext.get();
        TournamentEntry entry = registerTournamentEntryActivity.execute(cmd, userId);
        joinTournamentDiscussionActivity.execute(cmd.getTournamentId(), userId);
        return TournamentEntryAppConvertMapper.INSTANCE.toTournamentEntryDTO(entry.getData());
    }

    /** 整组替换本人报名偏好；终态报名不可修改。 */
    @Transactional
    public void update(TournamentEntryUpdateCmd cmd) {
        replaceEntryPreferenceActivity.execute(cmd);
    }

    /** 解冻本人报名，恢复等待匹配状态。 */
    @Transactional
    public void unfreeze(TournamentEntryUnfreezeCmd cmd) {
        unfreezeEntryActivity.execute(cmd);
    }

    /**
     * 支付报名费：校验 entry 为 PAYING 且赛事未满 → 建单 → 取拉起参数
     */
    @Transactional
    public PrepayDTO pay(TournamentEntryPayCmd cmd) {
        return prepareEntryPaymentActivity.execute(cmd);
    }

    /**
     * 退出赛事：置 WITHDRAWN（资格赛/正赛通用）；若正在比赛中，关闭比赛并让对手回匹配池
     */
    @Transactional
    public TournamentWithdrawResultDTO withdraw(TournamentWithdrawCmd cmd) {
        String userId = UserContext.get();
        withdrawTournamentEntryActivity.execute(cmd.getTournamentId(), userId);
        leaveTournamentDiscussionActivity.execute(cmd.getTournamentId(), userId);
        terminateWithdrawnMatchActivity.execute(cmd.getTournamentId(), userId);
        return new TournamentWithdrawResultDTO(false);
    }
}
