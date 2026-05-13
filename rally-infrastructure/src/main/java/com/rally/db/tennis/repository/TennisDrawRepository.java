package com.rally.db.tennis.repository;

import com.rally.db.tennis.service.TennisDrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class TennisDrawRepository {

    private final TennisDrawService tennisDrawService;

    public Long saveOrUpdate(String tournamentId, Integer year, String drawType, Integer size, Integer totalRounds) {
        return tennisDrawService.saveOrUpdate(tournamentId, year, drawType, size, totalRounds);
    }
}
