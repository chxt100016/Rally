package com.rally.domain.tennis.gateway;

import com.rally.domain.tennis.model.MatchData;
import com.rally.domain.tennis.model.SetScoreData;

import java.util.List;

public interface TennisMatchCollectGateway {
    List<MatchData> saveOrUpdateBatch(List<MatchData> matches);
    void saveSetScores(List<SetScoreData> setScores);
}
