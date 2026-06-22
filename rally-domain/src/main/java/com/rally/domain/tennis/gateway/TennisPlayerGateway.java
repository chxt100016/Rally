package com.rally.domain.tennis.gateway;

import com.rally.domain.tennis.model.PlayerData;

import java.util.List;

public interface TennisPlayerGateway {
    void saveOrUpdateBatch(List<PlayerData> players);
    List<PlayerData> listByTourOrderByRank(String tour);
    List<PlayerData> listByPlayerIds(List<String> playerIds);
}
