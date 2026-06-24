package com.rally.domain.tour.repository;

import com.rally.domain.tour.model.PlayerData;

import java.util.List;

public interface TourPlayerRepository {
    void saveOrUpdateBatch(List<PlayerData> players);
    List<PlayerData> listByTourOrderByRank(String tour);
    List<PlayerData> listByPlayerIds(List<String> playerIds);
}
