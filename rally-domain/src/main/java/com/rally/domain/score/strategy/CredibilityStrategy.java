package com.rally.domain.score.strategy;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.meetup.gateway.MeetupGateway;
import com.rally.domain.score.enums.ScoreDimensionEnum;
import com.rally.domain.score.model.ScoreChange;
import com.rally.domain.score.model.ScoreContext;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


/**
 * 可信度策略（全量重算分）
 * 三项加分求和后封顶 100：近90天完成约球 + 视频 + UTR
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CredibilityStrategy implements ScoreStrategy {

    private final MeetupGateway meetupGateway;
    private final TennisProfileGateway profileGateway;

    @Override
    public ScoreDimensionEnum dimension() {
        return ScoreDimensionEnum.CREDIBILITY;
    }

    @Override
    public ScoreChange calculate(ScoreContext ctx, String userId) {
        ScoreChange change = new ScoreChange();
        change.setDimension(ScoreDimensionEnum.CREDIBILITY);
        change.setRefId(ctx.getMeetupId());
        change.setReason(ChangeReasonEnum.SYSTEM);

        // 获取当前可信度
        Integer before = profileGateway.findByUserId(userId)
                .map(TennisProfileData::getCredibilityScore)
                .orElse(0);
        change.setBefore(before);

        // 1. 近窗口完成约球加分
        int matchWindowDays = SystemConfig.getInt(SystemConfigKey.SCORE_CREDIBILITY_MATCH_WINDOW_DAYS.getKey());
        long matchCount = meetupGateway.countFinishedMatches(userId, matchWindowDays);
        int matchPerScore = SystemConfig.getInt(SystemConfigKey.SCORE_CREDIBILITY_MATCH_PER_SCORE.getKey());
        int matchScoreCap = SystemConfig.getInt(SystemConfigKey.SCORE_CREDIBILITY_MATCH_SCORE_CAP.getKey());
        int sMatch = (int) Math.min(matchCount * matchPerScore, matchScoreCap);

        // 2. 视频加分
        int videoCount = profileGateway.findByUserId(userId)
                .map(p -> p.getVideos() != null ? p.getVideos().size() : 0)
                .orElse(0);
        int videoPerScore = SystemConfig.getInt(SystemConfigKey.SCORE_CREDIBILITY_VIDEO_PER_SCORE.getKey());
        int videoCap = SystemConfig.getInt(SystemConfigKey.SCORE_CREDIBILITY_VIDEO_CAP.getKey());
        int sVideo = Math.min(videoCount * videoPerScore, videoCap);

        // 3. UTR 加分（MVP 恒为 0）
        int sUtr = 0; // MVP 不实现

        // 4. 总和封顶 100
        int credibility = Math.min(sMatch + sVideo + sUtr, 100);

        // 5. 核查期遇「差」覆盖
        // TODO: 需要检查核查期状态，暂降为 penalty_credibility

        change.setAfter(credibility);
        change.setValue(credibility - before);

        return change;
    }
}
