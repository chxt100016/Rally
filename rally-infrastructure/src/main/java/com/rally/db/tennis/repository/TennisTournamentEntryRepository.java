package com.rally.db.tennis.repository;

import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.service.TennisTournamentEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TennisTournamentEntryRepository {

    private final TennisTournamentEntryService tennisTournamentEntryService;

    public void saveEntries(List<TennisTournamentEntryPO> entries) {
        tennisTournamentEntryService.saveEntries(entries);
    }

    public Map<String, Short> listSeedMapByDrawIds(List<Long> drawIds) {
        return tennisTournamentEntryService.listByDrawIds(drawIds)
                .stream()
                .filter(e -> e.getSeed() != null && e.getSeed() > 0)
                .collect(Collectors.toMap(
                        e -> e.getDrawId() + ":" + e.getPlayerId(),
                        TennisTournamentEntryPO::getSeed,
                        (a, b) -> a
                ));
    }
}
