package com.rally.domain.tour.gateway;

import com.rally.domain.tour.model.PlayerData;

import java.util.List;

public interface TourPlayerGateway {
    void saveOrUpdateBatch(List<PlayerData> players);
    List<PlayerData> listByTourOrderByRank(String tour);
    List<PlayerData> listByPlayerIds(List<String> playerIds);
}
