package com.rally.tournament.tournamentwithdraw.activity;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.tournament.shared.TournamentMatchRejectionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 terminate-withdrawn-match：终止退赛者的一场在途比赛并释放其他有效报名。
 */
@Component
@RequiredArgsConstructor
public class TerminateWithdrawnMatchActivity {

    private final TournamentMatchRepository matchRepository;
    private final TournamentMatchRejectionSupport rejectionSupport;

    /**
     * 与报名退赛、退出讨论共享事务；无在途比赛时按既有流程正常跳过。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId, String withdrawnUserId) {
        TournamentMatch match = matchRepository.findActiveMatchByTournamentAndUser(
                tournamentId, withdrawnUserId);
        if (match == null) {
            return;
        }
        match.getData().setStatus(TournamentMatchStatusEnum.REJECTED);
        rejectionSupport.persistMatch(match, false);
        rejectionSupport.settleRejectedMatch(match);
    }
}
