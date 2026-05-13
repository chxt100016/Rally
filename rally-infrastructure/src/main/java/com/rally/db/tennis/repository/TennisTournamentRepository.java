package com.rally.db.tennis.repository;

import com.rally.db.tennis.entity.TennisTournamentPO;
import com.rally.db.tennis.service.TennisTournamentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TennisTournamentRepository {

    private final TennisTournamentService tennisTournamentService;

    public void saveOrUpdateBatch(List<TennisTournamentPO> tournaments) {
        tennisTournamentService.saveOrUpdateBatch(tournaments);
    }

    public List<TennisTournamentPO> findActive() {
        return tennisTournamentService.findActive();
    }

    public List<TennisTournamentPO> findCurrentTournaments(LocalDate date) {
        return tennisTournamentService.findCurrentTournaments(date);
    }

    public List<TennisTournamentPO> listByCondition(String dbStatus, String tour,
                                                     LocalDate dateFrom, LocalDate dateTo) {
        return tennisTournamentService.listByCondition(dbStatus, tour, dateFrom, dateTo);
    }
}
