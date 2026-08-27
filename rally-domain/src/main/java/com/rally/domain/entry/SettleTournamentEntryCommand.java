package com.rally.domain.tournament.entry;

import java.time.LocalDateTime;

/** C6 已完成比赛对单个报名根的结算事实。 */
public record SettleTournamentEntryCommand(
        Outcome outcome,
        TournamentEntryRound completedRound,
        LocalDateTime completedTime) {

    public enum Outcome {
        WIN,
        LOSS
    }
}
