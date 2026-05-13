package com.rally.db.tennis.repository;

import com.rally.db.tennis.entity.TennisTournamentEntryPO;
import com.rally.db.tennis.service.TennisTournamentEntryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TennisTournamentEntryRepository {

    private final TennisTournamentEntryService tennisTournamentEntryService;

    public void saveEntries(List<TennisTournamentEntryPO> entries) {
        tennisTournamentEntryService.saveEntries(entries);
    }
}
