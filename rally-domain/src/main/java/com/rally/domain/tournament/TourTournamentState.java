package com.rally.domain.tour.tournament;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 与 {@code tour_tournament} 一行对应的不可变聚合状态。 */
public record TourTournamentState(
        Long id,
        String tournamentId,
        int year,
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
        LocalDate endDate,
        String imagePath,
        String backgroundPath,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TourTournamentIdentity identity() {
        return TourTournamentIdentity.fromSource(tournamentId, year);
    }

    TourTournamentProfile profile() {
        return new TourTournamentProfile(
                name,
                tour,
                category,
                surface,
                city,
                country,
                prizeMoney,
                prizeMoneyText,
                TourTournamentStatus.fromSource(status),
                startDate,
                endDate);
    }

    TourTournamentImageBinding imageBinding() {
        return TourTournamentImageBinding.restore(imagePath, backgroundPath);
    }

    TourTournamentState withGeneratedId(long generatedId) {
        return new TourTournamentState(
                generatedId,
                tournamentId,
                year,
                name,
                tour,
                category,
                surface,
                city,
                country,
                prizeMoney,
                prizeMoneyText,
                status,
                startDate,
                endDate,
                imagePath,
                backgroundPath,
                createTime,
                updateTime);
    }

    TourTournamentState withProfile(TourTournamentProfile profile) {
        return new TourTournamentState(
                id,
                tournamentId,
                year,
                profile.name(),
                profile.tour(),
                profile.category(),
                profile.surface(),
                profile.city(),
                profile.country(),
                profile.prizeMoney(),
                profile.prizeMoneyText(),
                profile.status().databaseValue(),
                profile.startDate(),
                profile.endDate(),
                imagePath,
                backgroundPath,
                createTime,
                updateTime);
    }

    TourTournamentState withImageBinding(TourTournamentImageBinding binding) {
        return new TourTournamentState(
                id,
                tournamentId,
                year,
                name,
                tour,
                category,
                surface,
                city,
                country,
                prizeMoney,
                prizeMoneyText,
                status,
                startDate,
                endDate,
                binding.imagePath(),
                binding.backgroundPath(),
                createTime,
                updateTime);
    }
}
