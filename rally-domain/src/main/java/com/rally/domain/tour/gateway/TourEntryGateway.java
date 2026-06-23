package com.rally.domain.tour.gateway;

import com.rally.domain.tour.model.TournamentEntryData;

import java.util.List;
import java.util.Map;

public interface TourEntryGateway {
    void saveEntries(List<TournamentEntryData> entries);
    Map<String, Short> listSeedMapByDrawIds(List<Long> drawIds);
    List<TournamentEntryData> listByDrawIds(List<Long> drawIds);
}
