package com.rally.db.tennis.repository;

import com.rally.db.tennis.entity.TennisSetScorePO;
import com.rally.db.tennis.service.TennisSetScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TennisSetScoreRepository {

    private final TennisSetScoreService tennisSetScoreService;

    public void saveOrUpdateBatch(List<TennisSetScorePO> scores) {
        tennisSetScoreService.saveOrUpdateBatch(scores);
    }
}
