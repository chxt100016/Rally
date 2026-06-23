package com.rally.domain.tour.gateway;

import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScoreData;

import java.util.List;

public interface TourMatchCollectGateway {
    List<MatchData> saveOrUpdateBatch(List<MatchData> matches);
    void saveSetScores(List<SetScoreData> setScores);
}
