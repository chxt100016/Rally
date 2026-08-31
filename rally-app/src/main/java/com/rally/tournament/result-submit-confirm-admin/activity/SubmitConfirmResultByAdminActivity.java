package com.rally.tournament.resultsubmitconfirmadmin.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.auth.exception.BusinessException;
import com.rally.domain.tournament.entry.SettleTournamentEntryCommand;
import com.rally.domain.tournament.entry.TournamentEntry;
import com.rally.domain.tournament.entry.TournamentEntryPersistence;
import com.rally.domain.tournament.entry.TournamentEntryRound;
import com.rally.domain.tournament.entry.TournamentEntryState;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.match.TournamentMatch;
import com.rally.domain.tournament.match.TournamentMatchCancellationTarget;
import com.rally.domain.tournament.match.TournamentMatchConfirmStatus;
import com.rally.domain.tournament.match.TournamentMatchDomainException;
import com.rally.domain.tournament.match.TournamentMatchParticipant;
import com.rally.domain.tournament.match.TournamentMatchPersistence;
import com.rally.domain.tournament.match.TournamentMatchState;
import com.rally.domain.tournament.match.TournamentMatchStatus;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 业务活动 submit-confirm-result-by-admin：运营按赛事编号和比赛序号指定获胜方，
 * 一次性代提交并代确认全部参与者赛果，完成比赛并结算胜负方报名。
 */
@Component
@RequiredArgsConstructor
public class SubmitConfirmResultByAdminActivity {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchPersistence matchPersistence;
    private final TournamentEntryPersistence entryPersistence;

    @Transactional(rollbackFor = Exception.class)
    public SubmitConfirmResultByAdminResult execute(String tournamentId, Integer matchNo, Integer winnerEntryNo) {
        // A1：只确认赛事身份存在，不要求赛事状态。
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);

        // A2：按自然键锁定读取最新比赛根及全部参与者。
        TournamentMatchCancellationTarget target = matchPersistence.findLatestByTournamentIdAndMatchNoForUpdate(tournamentId, matchNo);
        Assert.notNull(target, BizErrorCode.TOURNAMENT_MATCH_NOT_FOUND);
        TournamentMatchState state = target.state();

        // 幂等：比赛已完成且胜方与本次一致时直接返回既有结果，不重复提交或结算。
        if (state.status() == TournamentMatchStatus.COMPLETED && winnerEntryNo.equals(state.winnerEntryNo())) {
            return SubmitConfirmResultByAdminResult.completed(tournamentId, state.round(), state.winnerEntryNo(), state.completedTime());
        }

        Assert.isTrue(state.status() == TournamentMatchStatus.PENDING_PLAY || state.status() == TournamentMatchStatus.PENDING_CONFIRM, BizErrorCode.TOURNAMENT_INVALID_RESULT_CONFIRM);
        boolean winnerValid = target.participants().stream().anyMatch(participant -> participant.entryNo() == winnerEntryNo);
        Assert.isTrue(winnerValid, BizErrorCode.TOURNAMENT_RESULT_WINNER_INVALID);

        TournamentMatch match = TournamentMatch.restore(state, target.participants());
        String submitterUserId = target.participants().stream()
                .filter(participant -> participant.entryNo() == winnerEntryNo)
                .map(TournamentMatchParticipant::userId)
                .findFirst()
                .orElseThrow(() -> new BusinessException(BizErrorCode.TOURNAMENT_RESULT_WINNER_INVALID));

        LocalDateTime now = LocalDateTime.now();
        try {
            // A3：以胜方参赛编号下任一参与者为提交人提交赛果，覆盖已有胜方并把其余参与者重置为待确认。
            match.submitResult(submitterUserId, winnerEntryNo, now, match.state().version(), matchPersistence);

            // A4：逐个覆盖仍为待确认的参与者为已确认；已被记为已拒绝的参与者保持原状，不覆盖。
            for (TournamentMatchParticipant participant : match.participants()) {
                if (participant.resultConfirmStatus() == TournamentMatchConfirmStatus.PENDING) {
                    match.confirmResult(participant.userId(), now, match.state().version(), matchPersistence);
                }
            }
        } catch (TournamentMatchDomainException exception) {
            throw toBusinessException(exception);
        }
        // A5：以上每次命令均已按当前版本条件保存比赛根与全部参与关系。

        if (match.state().status() != TournamentMatchStatus.COMPLETED) {
            return SubmitConfirmResultByAdminResult.notCompleted();
        }

        // A6：比赛进入 COMPLETED 后，按胜负方结算对应参赛报名。
        settleEntries(match);

        return SubmitConfirmResultByAdminResult.completed(
                tournamentId,
                match.state().round(),
                match.state().winnerEntryNo(),
                match.state().completedTime());
    }

    private void settleEntries(TournamentMatch match) {
        Integer winnerEntryNo = match.state().winnerEntryNo();
        TournamentEntryRound round = TournamentEntryRound.valueOf(match.state().round().name());
        LocalDateTime completedTime = match.state().completedTime();

        for (TournamentMatchParticipant participant : match.participants()) {
            SettleTournamentEntryCommand.Outcome outcome = participant.entryNo() == winnerEntryNo
                    ? SettleTournamentEntryCommand.Outcome.WIN
                    : SettleTournamentEntryCommand.Outcome.LOSS;
            TournamentEntryState entryState = entryPersistence.findByTournamentAndUser(
                    match.state().tournamentId(), participant.userId());
            Assert.notNull(entryState, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
            TournamentEntry entry = TournamentEntry.restore(entryState);
            entry.settleCompletedMatch(
                    new SettleTournamentEntryCommand(outcome, round, completedTime),
                    entryPersistence);
        }
    }

    private BusinessException toBusinessException(TournamentMatchDomainException exception) {
        if (TournamentMatch.TOURNAMENT_MATCH_VERSION_CONFLICT.equals(exception.getErrorIdentifier())) {
            return new BusinessException(BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT, exception.getMessage());
        }
        if (TournamentMatch.TOURNAMENT_RESULT_WINNER_REQUIRED.equals(exception.getErrorIdentifier())) {
            return new BusinessException(BizErrorCode.TOURNAMENT_RESULT_WINNER_INVALID, exception.getMessage());
        }
        return new BusinessException(BizErrorCode.OPERATION_FAILED, exception.getMessage());
    }
}
