package com.rally.domain.tour.repository;

import com.rally.domain.tour.model.MatchData;
import com.rally.domain.tour.model.SetScoreData;

import java.util.List;

public interface TourMatchCollectRepository {
    List<MatchData> saveOrUpdateBatch(List<MatchData> matches);
    void saveSetScores(List<SetScoreData> setScores);
}
