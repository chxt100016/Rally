package com.rally.tournament.bookingreschedulerequest.activity;

import com.rally.domain.tournament.enums.RebookReasonEnum;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.tournament.shared.TournamentMatchRejectionSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 业务活动 request-rebooking：参与者打回已提交赛约，等待订场人重订。
 */
@Component
@RequiredArgsConstructor
public class RequestRebookingActivity {

    private final TournamentMatchRejectionSupport rejectionSupport;

    /**
     * 保持 schedule-confirm 重订分支的读取、校验、版本化保存及异常回滚顺序。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String participantUserId,
            RebookReasonEnum rebookReason) {
        TournamentMatch match = rejectionSupport.requireMatch(matchId);
        TournamentData tournament = rejectionSupport.requireTournament(
                match.getData().getTournamentId());
        TournamentEntry entry = rejectionSupport.requireEntry(
                match.getData().getTournamentId(), participantUserId);
        match.confirmSchedule(
                participantUserId,
                false,
                null,
                rebookReason,
                tournament.getQualifierRejectLimit(),
                tournament.getMainDrawRejectLimit(),
                entry.getData().getStage(),
                rejectionSupport.rejectCount(entry));
        rejectionSupport.persistMatch(match, true);
    }
}
