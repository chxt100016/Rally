package com.rally.domain.tour.repository;

import com.rally.domain.tour.model.TournamentData;

import java.time.LocalDate;
import java.util.List;

public interface TourTournamentRepository {
    List<TournamentData> findCurrentTournaments(LocalDate date);
    boolean exists(String tournamentId);
    void saveOrUpdateBatch(List<TournamentData> tournaments);
    List<TournamentData> listByCondition(String status, String tour, LocalDate dateFrom, LocalDate dateTo);
    void updateImagePaths(String tournamentId, String imagePath, String backgroundPath);
    List<TournamentData> listByTournamentIds(List<String> tournamentIds);
    TournamentData findByTournamentId(String tournamentId);
    List<TournamentData> listPendingBackground(LocalDate dateFrom, LocalDate dateTo);
}
