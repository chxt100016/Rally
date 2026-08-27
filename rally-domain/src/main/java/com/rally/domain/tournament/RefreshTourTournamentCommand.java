package com.rally.domain.tour.tournament;

import java.time.LocalDate;

/** C1：新增或整体刷新一份职业赛事年度来源主档。 */
public record RefreshTourTournamentCommand(
        String tournamentId,
        Integer year,
        String name,
        String tour,
        String category,
        String surface,
        String city,
        String country,
        Integer prizeMoney,
        String prizeMoneyText,
        String status,
        LocalDate startDate,
        LocalDate endDate) {
}
