package com.rally.domain.tour.gateway;

import com.rally.domain.tour.model.TourDrawData;

import java.util.List;

public interface TourDrawGateway {
    Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds);
    List<TourDrawData> listByTournamentIds(List<String> tournamentIds);
}
