package com.rally.tournament.resultconfirm.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
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
 * 业务活动 confirm-result：记录本人赛果确认，全员确认后完成比赛并结算赛事进度。
 */
@Component
@RequiredArgsConstructor
public class ConfirmResultActivity {

    private final TournamentMatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final TournamentEntryRepository entryRepository;
    private final TournamentRoundProgressDecisionService roundProgressDecisionService;

    /**
     * 保持原 confirm=true 链路的读取顺序、乐观锁和跨聚合事务；本分支不发送通知。
     */
    @Transactional(rollbackFor = Exception.class)
    public void execute(
            String matchId,
            String participantUserId,
            LocalDateTime confirmedTime) {
        Assert.notNull(confirmedTime, BizErrorCode.PARAM_ERROR);

        // A1：比赛、赛事、本人报名及参与关系继续暴露原有错误语义。
        TournamentMatch match = matchRepository.findByBizIdWithParticipants(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);

        Tournament tournament = getTournament(match.getData().getTournamentId());
        TournamentEntry participantEntry = getUserEntry(
                match.getData().getTournamentId(), participantUserId);

        int rejectCount = participantEntry.getData().getStage()
                == com.rally.domain.tournament.enums.TournamentEntryStageEnum.QUALIFY
                ? participantEntry.getData().getQualifierRejectCount()
                : participantEntry.getData().getMainDrawRejectCount();

        // A2-A3：固定 confirm=true；重复确认刷新本人时间，最后一人同时完成比赛。
        match.confirmResult(
                participantUserId,
                true,
                null,
                tournament.getData().getQualifierRejectLimit(),
                tournament.getData().getMainDrawRejectLimit(),
                participantEntry.getData().getStage(),
                rejectCount);
        replaceConfirmedTime(match, participantUserId, confirmedTime);
        if (match.getData().getStatus() == TournamentMatchStatusEnum.COMPLETED) {
            match.getData().setCompletedTime(confirmedTime);
        }

        boolean updated = matchRepository.updateWithVersion(match.getData());
        if (!updated) {
            throw new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        }
        matchRepository.saveParticipants(match.getParticipants());

        if (match.getData().getStatus() != TournamentMatchStatusEnum.COMPLETED) {
            return;
        }

        // A4-A5：只在全员确认后结算胜负；决赛结束赛事，其他轮次评估单向推进。
        settleEntries(match);
        if (match.getData().getRound() == TournamentRoundEnum.FINAL) {
            tournament.finish(
                    match.getData().getWinnerEntryNo(),
                    match.getData().getCompletedTime());
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

    private TournamentEntry getUserEntry(String tournamentId, String userId) {
        TournamentEntryData entryData = entryRepository.findByTournamentAndUser(
                tournamentId, userId);
        Assert.notNull(entryData, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(entryData);
    }

    private void replaceConfirmedTime(
            TournamentMatch match,
            String participantUserId,
            LocalDateTime confirmedTime) {
        for (MatchParticipantData participant : match.getParticipants()) {
            if (participantUserId.equals(participant.getUserId())) {
                participant.setResultConfirmTime(confirmedTime);
                return;
            }
        }
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
            TournamentEntry entry = getUserEntry(
                    match.getData().getTournamentId(), participant.getUserId());
            entry.advanceAfterWin(match.getData().getRound());
            entryRepository.save(entry.getData());
        }
        for (MatchParticipantData participant : losers) {
            TournamentEntry entry = getUserEntry(
                    match.getData().getTournamentId(), participant.getUserId());
            entry.getData().setStatus(
                    match.getData().getRound() == TournamentRoundEnum.QUALIFIER
                            ? TournamentEntryStatusEnum.WAITING
                            : TournamentEntryStatusEnum.ELIMINATED);
            entryRepository.save(entry.getData());
        }
    }
}
