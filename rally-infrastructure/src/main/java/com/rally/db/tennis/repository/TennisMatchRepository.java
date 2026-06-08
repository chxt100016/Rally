package com.rally.db.tennis.repository;

import com.rally.db.tennis.entity.TennisMatchPO;
import com.rally.db.tennis.service.TennisMatchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class TennisMatchRepository {

    private final TennisMatchService tennisMatchService;

    public void saveOrUpdateBatch(List<TennisMatchPO> matches) {
        tennisMatchService.saveOrUpdateBatch(matches);
    }

    public void updateBatchById(List<TennisMatchPO> matches) {
        tennisMatchService.updateBatchById(matches);
    }

    public List<TennisMatchPO> lambdaQuery(List<String> matchIds) {
        return tennisMatchService.lambdaQuery()
                .in(TennisMatchPO::getMatchId, matchIds)
                .list();
    }

    public List<TennisMatchPO> findActiveByTournament(String tournamentId, Integer year) {
        return tennisMatchService.findActiveByTournament(tournamentId, year);
    }

    public boolean hasActiveMatches() {
        return tennisMatchService.hasActiveMatches();
    }

    public List<TennisMatchPO> findByTournamentIdsAndDate(List<String> tournamentIds, LocalDate date) {
        return tennisMatchService.findByTournamentIdsAndDate(tournamentIds, date);
    }
}
