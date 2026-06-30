package com.rally.domain.recap.service;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.rally.domain.auth.enums.BizErrorCode;
import com.rally.domain.meetup.model.Meetup;
import com.rally.domain.recap.convert.ScoreConvertMapper;
import com.rally.domain.recap.gateway.ScoreRepository;
import com.rally.domain.recap.model.ScoreAddCmd;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreUpdateCmd;
import com.rally.domain.user.model.UserProfile;
import com.rally.domain.user.service.UserProfileDomainService;
import com.rally.domain.utils.Assert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreDomainService {

    private final ScoreRepository scoreRepository;
    private final UserProfileDomainService userProfileDomainService;

    private static final ScoreConvertMapper MAPPER = ScoreConvertMapper.INSTANCE;

    public void addScoreItem(Meetup meetup, String userId, ScoreAddCmd cmd, LocalDateTime meetupDate, String venueName) {
        String meetupId = meetup.getMeetupId();

        // 同场同盘唯一校验
        boolean setExists = scoreRepository.existsByMeetupAndSet(meetupId, cmd.getSetNum());
        Assert.isTrue(!setExists, BizErrorCode.SCORE_SET_DUPLICATE);

        // 构建比分记录，计算胜负边
        ScoreRecordData data = MAPPER.toScoreRecordData(cmd, IdWorker.getIdStr(), meetupId, userId, meetupDate, venueName);
        data.setWinSide(TennisScorePolicy.calcWinSide(cmd.getSideAScore(), cmd.getSideBScore()));

        // 填充选手昵称和头像（冗余存储）
        List<String> playerIds = Stream.of(cmd.getSideAPlayer1(), cmd.getSideAPlayer2(), cmd.getSideBPlayer1(), cmd.getSideBPlayer2()).filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(playerIds);
        MAPPER.fillUserinfo(data, profileMap);

        scoreRepository.addScore(data);
        // 触发评分重算 TODO
    }

    public void updateScoreItem(Meetup meetup, String userId, ScoreUpdateCmd cmd, LocalDateTime meetupDate, String venueName) {
        String meetupId = meetup.getMeetupId();

        // 乐观锁校验：记录存在且版本一致
        ScoreRecordData existing = scoreRepository.findByBizId(meetupId, cmd.getBizId());
        Assert.notNull(existing, BizErrorCode.RECAP_SCORE_NOT_FOUND);
        Assert.isTrue(existing.getVersion().equals(cmd.getVersion()), BizErrorCode.SCORE_VERSION_MISMATCH);

        // 构建新比分记录，计算胜负边
        ScoreRecordData data = MAPPER.toScoreRecordData(cmd, meetupId, userId, meetupDate, venueName);
        data.setWinSide(TennisScorePolicy.calcWinSide(cmd.getSideAScore(), cmd.getSideBScore()));

        // 填充选手昵称和头像（冗余存储）
        List<String> playerIds = Stream.of(cmd.getSideAPlayer1(), cmd.getSideAPlayer2(), cmd.getSideBPlayer1(), cmd.getSideBPlayer2()).filter(StringUtils::isNotBlank).distinct().toList();
        Map<String, UserProfile> profileMap = userProfileDomainService.listMap(playerIds);
        MAPPER.fillUserinfo(data, profileMap);

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
