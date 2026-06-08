package com.rally.db.tennis.repository;

import com.rally.db.tennis.entity.TennisPlayerPO;
import com.rally.db.tennis.service.TennisPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TennisPlayerRepository {

    private final TennisPlayerService tennisPlayerService;

    public void saveOrUpdateBatch(List<TennisPlayerPO> players) {
        tennisPlayerService.saveOrUpdateBatch(players);
    }

    public List<TennisPlayerPO> listByTourOrderByRank(String tour) {
        return tennisPlayerService.lambdaQuery()
                .eq(TennisPlayerPO::getTour, tour)
                .isNotNull(TennisPlayerPO::getRank)
                .orderByAsc(TennisPlayerPO::getRank)
                .list();
    }

    public List<TennisPlayerPO> listByPlayerIds(List<String> playerIds) {
        return tennisPlayerService.listByPlayerIds(playerIds);
    }
}
