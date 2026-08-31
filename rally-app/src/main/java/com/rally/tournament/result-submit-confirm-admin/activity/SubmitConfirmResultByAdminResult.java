package com.rally.tournament.resultsubmitconfirmadmin.activity;

import com.rally.domain.tournament.match.TournamentMatchRound;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * submit-confirm-result-by-admin 活动的执行结果，供 advance-tournament-progress 活动
 * 决定决赛收口或轮次推进。
 */
@Getter
public class SubmitConfirmResultByAdminResult {

    private final boolean matchCompleted;
    private final String tournamentId;
    private final TournamentMatchRound round;
    private final Integer winnerEntryNo;
    private final LocalDateTime completedTime;

    private SubmitConfirmResultByAdminResult(boolean matchCompleted, String tournamentId, TournamentMatchRound round, Integer winnerEntryNo, LocalDateTime completedTime) {
        this.matchCompleted = matchCompleted;
        this.tournamentId = tournamentId;
        this.round = round;
        this.winnerEntryNo = winnerEntryNo;
        this.completedTime = completedTime;
    }

    public static SubmitConfirmResultByAdminResult completed(String tournamentId, TournamentMatchRound round, Integer winnerEntryNo, LocalDateTime completedTime) {
        return new SubmitConfirmResultByAdminResult(true, tournamentId, round, winnerEntryNo, completedTime);
    }

    public static SubmitConfirmResultByAdminResult notCompleted() {
        return new SubmitConfirmResultByAdminResult(false, null, null, null, null);
    }
}
