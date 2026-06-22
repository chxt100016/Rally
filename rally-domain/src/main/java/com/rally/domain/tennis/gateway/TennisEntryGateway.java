package com.rally.domain.tennis.gateway;

import com.rally.domain.tennis.model.TournamentEntryData;

import java.util.List;
import java.util.Map;

public interface TennisEntryGateway {
    void saveEntries(List<TournamentEntryData> entries);
    Map<String, Short> listSeedMapByDrawIds(List<Long> drawIds);
    List<TournamentEntryData> listByDrawIds(List<Long> drawIds);
}
