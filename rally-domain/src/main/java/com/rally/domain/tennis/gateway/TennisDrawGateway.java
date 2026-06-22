package com.rally.domain.tennis.gateway;

import com.rally.domain.tennis.model.TennisDrawData;

import java.util.List;

public interface TennisDrawGateway {
    Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds);
    List<TennisDrawData> listByTournamentIds(List<String> tournamentIds);
}
