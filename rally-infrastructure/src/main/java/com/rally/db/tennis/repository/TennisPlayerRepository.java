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
}
