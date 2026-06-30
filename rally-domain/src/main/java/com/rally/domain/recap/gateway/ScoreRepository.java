package com.rally.domain.recap.gateway;

import com.rally.domain.recap.model.ScoreRecordData;

import java.util.List;

public interface ScoreRepository {

    boolean existsByMeetupAndSet(String meetupId, Integer setNumber);

    ScoreRecordData findByBizId(String meetupId, String bizId);

    void addScore(ScoreRecordData data);

    void updateScore(ScoreRecordData data);

    void deleteScore(String meetupId, String bizId);

    List<ScoreRecordData> listScoresByMeetup(String meetupId);

    List<ScoreRecordData> listScoresByUserId(String userId);
}
