package com.rally.domain.tour.repository;

import com.rally.domain.tour.model.TourDrawData;

import java.util.List;

public interface TourDrawRepository {
    Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds);
    List<TourDrawData> listByTournamentIds(List<String> tournamentIds);
}
