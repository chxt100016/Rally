package com.rally.tournament.resultconfirmadmin.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.roundprogress.RoundProgressDecision;
import com.rally.domain.tournament.roundprogress.RoundProgressRejection;
import com.rally.domain.tournament.roundprogress.RoundProgressResult;
import com.rally.domain.tournament.roundprogress.TournamentRoundProgressDecisionService;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务活动 advance-tournament-progress：决赛完成时结束赛事并记录冠军；
 * 非决赛时按完赛进度评估并推进赛事当前轮次。
 */
@Component
@RequiredArgsConstructor
public class AdvanceTournamentProgressActivity {

    private final TournamentRepository tournamentRepository;
    private final TournamentRoundProgressDecisionService roundProgressDecisionService;

    @Transactional(rollbackFor = Exception.class)
    public void execute(String tournamentId, TournamentRoundEnum round, Integer winnerEntryNo, LocalDateTime completedTime) {
        // A1-A2：决赛时直接完成赛事并记录冠军，不再执行轮次评估。
        if (round == TournamentRoundEnum.FINAL) {
            TournamentData tournamentData = tournamentRepository.findByBizId(tournamentId);
            Assert.notNull(tournamentData, BizErrorCode.TOURNAMENT_NOT_FOUND);
            Tournament tournament = new Tournament(tournamentData);
            tournament.finish(winnerEntryNo, completedTime);
            tournamentRepository.save(tournament.getData());
            return;
        }

        // A3-A4：非决赛时按完赛进度评估目标轮次，ADVANCE 时单向推进。
        RoundProgressResult result = roundProgressDecisionService.evaluate(tournamentId);
        if (!result.isAccepted()) {
            throw roundProgressRejection(result.getRejection());
        }
        if (result.getDecision() != RoundProgressDecision.ADVANCE) {
            return;
        }
        TournamentRoundEnum targetRound = result.getTargetRound();
        if (targetRound == null) {
            throw new BusinessException(BizErrorCode.OPERATION_FAILED, "轮次推进目标不能为空");
        }
        tournamentRepository.advanceCurrentRoundIfLater(tournamentId, targetRound);
    }

    private BusinessException roundProgressRejection(RoundProgressRejection rejection) {
        if (rejection == RoundProgressRejection.TOURNAMENT_NOT_FOUND) {
            return new BusinessException(BizErrorCode.TOURNAMENT_NOT_FOUND);
        }
        return new BusinessException(BizErrorCode.OPERATION_FAILED, "轮次推进目标不能为空");
    }
}
