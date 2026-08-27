package com.rally.tournament.resultsubmit.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务活动 submit-result：记录胜方并重置全部参与者的赛果确认状态。
 */
@Component
@RequiredArgsConstructor
public class SubmitResultActivity {

    private final TournamentMatchRepository matchRepository;

    /**
     * 保持原 submit-result 链路的参与者、胜方和版本冲突语义。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String participantUserId,
            Integer winnerEntryNo,
            LocalDateTime submittedTime) {
        Assert.notNull(submittedTime, BizErrorCode.PARAM_ERROR);

        // A1-A2：整体加载比赛与参与者，由既有聚合保持状态、身份和胜方校验。
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        match.submitResult(participantUserId, winnerEntryNo);

        // A3-A4：活动时间是赛果提交及提交人确认的同一时间事实。
        match.getData().setSubmittedTime(submittedTime);
        replaceSubmitterConfirmationTime(
                match, participantUserId, submittedTime);

        boolean updated = matchRepository.updateWithVersion(match.getData());
        if (!updated) {
            throw new BusinessException(
                    BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());
    }

    private void replaceSubmitterConfirmationTime(
            TournamentMatch match,
            String participantUserId,
            LocalDateTime submittedTime) {
        for (MatchParticipantData participant : match.getParticipants()) {
            if (participantUserId.equals(participant.getUserId())) {
                participant.setResultConfirmTime(submittedTime);
                return;
            }
        }
    }
}
