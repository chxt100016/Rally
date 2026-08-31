package com.rally.tournament.resultconfirmadmin.activity;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import com.rally.domain.tournament.gateway.TournamentEntryRepository;
import com.rally.domain.tournament.gateway.TournamentMatchRepository;
import com.rally.domain.tournament.gateway.TournamentRepository;
import com.rally.domain.tournament.model.MatchParticipantData;
import com.rally.domain.tournament.model.TournamentData;
import com.rally.domain.tournament.model.TournamentEntry;
import com.rally.domain.tournament.model.TournamentEntryData;
import com.rally.domain.tournament.model.TournamentMatch;
import com.rally.domain.tournament.model.TournamentMatchData;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 业务活动 confirm-result-by-admin：运营按赛事编号和比赛序号一次性代确认全部待确认参与者赛果，
 * 完成比赛并结算胜负方报名。
 */
@Component
@RequiredArgsConstructor
public class ConfirmResultByAdminActivity {

    private final TournamentRepository tournamentRepository;
    private final TournamentMatchRepository matchRepository;
    private final TournamentEntryRepository entryRepository;

    @Transactional(rollbackFor = Exception.class)
    public ConfirmResultByAdminResult execute(String tournamentId, Integer matchNo) {
        // A1：只确认赛事身份存在，不要求赛事状态。
        TournamentData tournament = tournamentRepository.findByBizId(tournamentId);
        Assert.notNull(tournament, BizErrorCode.TOURNAMENT_NOT_FOUND);

        // A2：按自然键定位比赛后锁定读取最新根及全部参与者；比赛不是 PENDING_CONFIRM 或无胜方时拒绝。
        String matchId = locateMatchId(tournamentId, matchNo);
        Assert.notNull(matchId, BizErrorCode.TOURNAMENT_MATCH_NOT_FOUND);
        TournamentMatch match = matchRepository.findByBizIdWithParticipantsForUpdate(matchId);
        Assert.notNull(match, BizErrorCode.TOURNAMENT_MATCH_NOT_FOUND);
        Assert.eq(match.getData().getStatus(), TournamentMatchStatusEnum.PENDING_CONFIRM, BizErrorCode.TOURNAMENT_INVALID_RESULT_CONFIRM);
        Assert.notNull(match.getData().getWinnerEntryNo(), BizErrorCode.TOURNAMENT_RESULT_WINNER_REQUIRED);

        // A3：逐个覆盖仍为 PENDING 的参与者为 CONFIRMED 并刷新确认时间；REJECTED 的保持原状，不覆盖。
        LocalDateTime now = LocalDateTime.now();
        for (MatchParticipantData participant : match.getParticipants()) {
            if (participant.getResultConfirmStatus() == ConfirmStatusEnum.PENDING) {
                match.confirmResult(participant.getUserId(), true, null, 0, 0, null, 0);
                participant.setResultConfirmTime(now);
            }
        }
        // 兜底：参与者已全部 CONFIRMED 但比赛仍停留在 PENDING_CONFIRM 的存量数据，直接推进为已完成。
        match.advanceIfAllResultConfirmed();

        // A4：以当前版本统一保存比赛根与全部参与关系；全员确认时比赛已进入 COMPLETED。
        boolean updated = matchRepository.updateWithVersion(match.getData());
        Assert.isTrue(updated, BizErrorCode.TOURNAMENT_MATCH_VERSION_CONFLICT);
        matchRepository.saveParticipants(match.getParticipants());

        if (match.getData().getStatus() != TournamentMatchStatusEnum.COMPLETED) {
            return ConfirmResultByAdminResult.notCompleted();
        }

        // A5：比赛进入 COMPLETED 后，按胜负方结算对应参赛报名。
        settleEntries(match);

        return ConfirmResultByAdminResult.completed(
                tournamentId,
                match.getData().getRound(),
                match.getData().getWinnerEntryNo(),
                match.getData().getCompletedTime());
    }

    private String locateMatchId(String tournamentId, Integer matchNo) {
        return matchRepository.findByTournamentId(tournamentId).stream()
                .filter(data -> matchNo.equals(data.getMatchNo()))
                .map(TournamentMatchData::getBizId)
                .findFirst()
                .orElse(null);
    }

    private void settleEntries(TournamentMatch match) {
        Integer winnerEntryNo = match.getData().getWinnerEntryNo();
        TournamentRoundEnum round = match.getData().getRound();

        List<MatchParticipantData> winners = match.getParticipants().stream()
                .filter(participant -> winnerEntryNo.equals(participant.getEntryNo()))
                .toList();
        List<MatchParticipantData> losers = match.getParticipants().stream()
                .filter(participant -> !winnerEntryNo.equals(participant.getEntryNo()))
                .toList();

        for (MatchParticipantData participant : winners) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), participant.getUserId());
            entry.advanceAfterWin(round);
            entryRepository.save(entry.getData());
        }
        for (MatchParticipantData participant : losers) {
            TournamentEntry entry = getUserEntry(match.getData().getTournamentId(), participant.getUserId());
            entry.advanceAfterLoss(round);
            entryRepository.save(entry.getData());
        }
    }

    private TournamentEntry getUserEntry(String tournamentId, String userId) {
        TournamentEntryData entryData = entryRepository.findByTournamentAndUser(tournamentId, userId);
        Assert.notNull(entryData, BizErrorCode.TOURNAMENT_ENTRY_NOT_FOUND);
        return new TournamentEntry(entryData);
    }
}
