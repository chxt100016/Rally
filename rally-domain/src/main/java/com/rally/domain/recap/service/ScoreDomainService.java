package com.rally.domain.recap.service;

import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.recap.gateway.RecapRepository;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreDomainService {

    private final RecapRepository recapRepository;

    public void addScoreItem(Meetup meetup, String userId, ScoreAddCmd cmd, LocalDateTime meetupDate, String venueName) {
        recapRepository.addScore(meetup.getMeetupId(), userId, cmd, meetupDate, venueName);
        // 触发评分重算 TODO
    }

    public void updateScoreItem(Meetup meetup, String userId, ScoreUpdateCmd cmd, LocalDateTime meetupDate, String venueName) {
        recapRepository.updateScore(meetup.getMeetupId(), userId, cmd, meetupDate, venueName);
        // 触发评分重算 TODO
    }

    public void deleteScoreItem(Meetup meetup, String bizId) {
        recapRepository.deleteScore(meetup.getMeetupId(), bizId);
        // 触发评分重算 TODO
    }

    public List<ScoreRecordData> listScoresByMeetup(String meetupId) {
        return recapRepository.listScoresByMeetup(meetupId);
    }

    public List<ScoreRecordData> listScoresByUserId(String userId) {
        return recapRepository.listScoresByUserId(userId);
    }
}
