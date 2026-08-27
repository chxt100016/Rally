package com.rally.tournament.resultconfirmationtimeout.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.Tournament;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.roundprogress.RoundProgressDecision;
import com.rally.domain.tournament.roundprogress.RoundProgressRejection;
import com.rally.domain.tournament.roundprogress.RoundProgressResult;
import com.rally.domain.tournament.roundprogress.TournamentRoundProgressDecisionService;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务活动 complete-timeout-result：自动完成一场确认超时的赛果并统一结算。
 */
@Component
@RequiredArgsConstructor
public class CompleteTimeoutResultActivity {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
    private final TournamentRoundProgressDecisionService roundProgressDecisionService;

    /**
     * 外层任务负责固定 48 小时筛选和逐场异常隔离；本方法只承载单场事务。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(String matchId, LocalDateTime completedTime) {
        Assert.notNull(completedTime, BizErrorCode.PARAM_ERROR);

        // A1：重新加载最新状态；候选已被其他流程处理时幂等跳过。
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        if (match.getData().getStatus() != TournamentMatchStatusEnum.PENDING_CONFIRM) {
            return;
        }

        // A2-A3：仅补齐待确认参与者，保留 CONFIRMED/REJECTED 等既有状态。
        for (MatchParticipantData participant : match.getParticipants()) {
            if (participant.getResultConfirmStatus() == ConfirmStatusEnum.PENDING) {
                participant.setResultConfirmStatus(ConfirmStatusEnum.CONFIRMED);
                participant.setResultConfirmTime(completedTime);
            }
        }
        match.getData().setStatus(TournamentMatchStatusEnum.COMPLETED);
        match.getData().setCompletedTime(completedTime);

        boolean updated = matchRepository.updateWithVersion(match.getData());
        if (!updated) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        settleEntries(match);

        // A4：以比赛自身轮次判定决赛；普通轮次沿用统一轮次评估服务。
        if (match.getData().getRound() == TournamentRoundEnum.FINAL) {
            Tournament tournament = getTournament(match.getData().getTournamentId());
            tournament.finish(match.getData().getWinnerEntryNo(), completedTime);
            tournamentRepository.save(tournament.getData());
        } else {
            advanceRoundIfReady(match.getData().getTournamentId());
        }
    }

    private void advanceRoundIfReady(String tournamentId) {
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
        return new BusinessException(BizErrorCode.TOURNAMENT_CONFIG_INCOMPLETE,
                "正赛签位数只能是2到64的2次方");
    }

    private Tournament getTournament(String tournamentId) {
        TournamentData tournamentData = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournamentData, BizErrorCode.TOURNAMENT_NOT_FOUND);
        return new Tournament(tournamentData);
    }

    private TournamentEntry getEntry(String tournamentId, String userId) {
        TournamentEntryData entryData = entryRepository.findByTournamentAndUser(
                tournamentId, userId);
        Assert.notNull(entryData, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(entryData);
    }

    private void settleEntries(TournamentMatch match) {
        Integer winnerEntryNo = match.getData().getWinnerEntryNo();
        Assert.notNull(winnerEntryNo, BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);

        List<MatchParticipantData> winners = match.getParticipants().stream()
                .filter(participant -> winnerEntryNo.equals(participant.getEntryNo()))
                .toList();
        List<MatchParticipantData> losers = match.getParticipants().stream()
                .filter(participant -> !winnerEntryNo.equals(participant.getEntryNo()))
                .toList();

        for (MatchParticipantData participant : winners) {
            TournamentEntry entry = getEntry(
                    match.getData().getTournamentId(), participant.getUserId());
            entry.advanceAfterWin(match.getData().getRound());
            entryRepository.save(entry.getData());
        }
        for (MatchParticipantData participant : losers) {
            TournamentEntry entry = getEntry(
                    match.getData().getTournamentId(), participant.getUserId());
            entry.getData().setStatus(
                    match.getData().getRound() == TournamentRoundEnum.QUALIFIER
                            ? TournamentEntryStatusEnum.WAITING
                            : TournamentEntryStatusEnum.ELIMINATED);
            entryRepository.save(entry.getData());
        }
    }
}
