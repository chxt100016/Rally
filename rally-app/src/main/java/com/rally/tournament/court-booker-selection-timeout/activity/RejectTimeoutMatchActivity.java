package com.rally.tournament.courtbookerselectiontimeout.activity;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.tournament.shared.TournamentMatchRejectionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 reject-timeout-match：终止待选订场人超时的比赛并释放有效报名。
 */
@Component
@RequiredArgsConstructor
public class RejectTimeoutMatchActivity {

    private final TournamentMatchRejectionSupport rejectionSupport;

    /**
     * 每个候选比赛单独调用本事务；扫描阈值和单场失败隔离由外层定时任务保持。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String matchId) {
        TournamentMatch match = rejectionSupport.requireMatch(matchId);
        if (match.getData().getStatus() != TournamentMatchStatusEnum.MATCHED) {
            return;
        }
        match.getData().setStatus(TournamentMatchStatusEnum.REJECTED);
        match.getData().setRejectReasonCode("TIMEOUT");
        rejectionSupport.persistMatch(match, false);
        rejectionSupport.settleRejectedMatch(match);
    }
}
