package com.rally.domain.recap.service;

import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.recap.factory.ScoreRecordFactory;
import com.rally.domain.recap.gateway.ScoreRepository;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreDomainService {

    private final ScoreRepository scoreRepository;
    private final ScoreRecordFactory scoreRecordFactory;

    public void addScoreItem(Meetup meetup, String userId, ScoreAddCmd cmd) {
        String meetupId = meetup.getMeetupId();

        // 同场同盘唯一校验
        boolean setExists = scoreRepository.existsByMeetupAndSet(meetupId, cmd.getSetNum());
        Assert.isTrue(!setExists, BizErrorCode.SCORE_SET_DUPLICATE);

        ScoreRecordData data = scoreRecordFactory.create(cmd, meetup, userId);

        scoreRepository.addScore(data);
        // 触发评分重算 TODO
    }

    public void updateScoreItem(Meetup meetup, String userId, ScoreUpdateCmd cmd) {
        String meetupId = meetup.getMeetupId();

        // 乐观锁校验：记录存在且版本一致
        ScoreRecordData existing = scoreRepository.findByBizId(meetupId, cmd.getBizId());
        Assert.notNull(existing, BizErrorCode.RECAP_SCORE_NOT_FOUND);
        Assert.isTrue(existing.getVersion().equals(cmd.getVersion()), BizErrorCode.SCORE_VERSION_MISMATCH);

        ScoreRecordData data = scoreRecordFactory.create(cmd, meetup, userId);

        scoreRepository.updateScore(data);
        // 触发评分重算 TODO
    }

    public void deleteScoreItem(Meetup meetup, String bizId) {
        scoreRepository.deleteScore(meetup.getMeetupId(), bizId);
        // 触发评分重算 TODO
    }

    public List<ScoreRecordData> listScoresByMeetup(String meetupId) {
        return scoreRepository.listScoresByMeetup(meetupId);
    }

    public List<ScoreRecordData> listScoresByUserId(String userId) {
        return scoreRepository.listScoresByUserId(userId);
    }
}
