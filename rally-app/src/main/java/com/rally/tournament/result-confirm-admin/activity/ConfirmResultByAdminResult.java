package com.rally.tournament.resultconfirmadmin.activity;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * confirm-result-by-admin 活动的执行结果，供 advance-tournament-progress 活动决定决赛收口或轮次推进。
 */
@Getter
public class ConfirmResultByAdminResult {

    private final boolean matchCompleted;
    private final String tournamentId;
    private final TournamentRoundEnum round;
    private final Integer winnerEntryNo;
    private final LocalDateTime completedTime;

    private ConfirmResultByAdminResult(boolean matchCompleted, String tournamentId, TournamentRoundEnum round, Integer winnerEntryNo, LocalDateTime completedTime) {
        this.matchCompleted = matchCompleted;
        this.tournamentId = tournamentId;
        this.round = round;
        this.winnerEntryNo = winnerEntryNo;
        this.completedTime = completedTime;
    }

    public static ConfirmResultByAdminResult completed(String tournamentId, TournamentRoundEnum round, Integer winnerEntryNo, LocalDateTime completedTime) {
        return new ConfirmResultByAdminResult(true, tournamentId, round, winnerEntryNo, completedTime);
    }

    public static ConfirmResultByAdminResult notCompleted() {
        return new ConfirmResultByAdminResult(false, null, null, null, null);
    }
}
