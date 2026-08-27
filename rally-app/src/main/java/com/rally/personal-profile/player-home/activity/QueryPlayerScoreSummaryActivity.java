package com.rally.personalprofile.playerhome.activity;

import com.rally.config.property.QiniuConfiguration;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.meetup.enums.ResultTypeEnum;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.service.ScoreDomainService;
import com.rally.domain.user.model.MyProfileSetScoreDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 业务活动 query-player-score-summary：汇总目标球员的盘级比分。
 */
@Component
@RequiredArgsConstructor
public class QueryPlayerScoreSummaryActivity {

    private static final int RECENT_SET_LIMIT = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final ScoreDomainService scoreDomainService;

    public MyProfileSetScoreDTO execute(String targetUserId) {
        // A1：仓储按四个球员位置匹配目标用户，并按 biz_id 倒序返回全量比分。
        List<ScoreRecordData> records = scoreDomainService.listScoresByUserId(targetUserId);

        // A2：total 包含 RALLY，单打和双打只按各自持久化枚举统计。
        long singleCount = records.stream()
                .filter(record -> record.getMatchType() == MatchTypeEnum.SINGLE)
                .count();
        long doubleCount = records.stream()
                .filter(record -> record.getMatchType() == MatchTypeEnum.DOUBLE)
                .count();

        // A3：只转换最新十盘，保留双打空位与抢七空值。
        List<MyProfileSetScoreDTO.SetItem> setItems = records.stream()
                .limit(RECENT_SET_LIMIT)
                .map(record -> toSetItem(targetUserId, record))
                .toList();

        return new MyProfileSetScoreDTO()
                .setTotal((long) records.size())
                .setSingleCount(singleCount)
                .setDoubleCount(doubleCount)
                .setSetItems(setItems);
    }

    private MyProfileSetScoreDTO.SetItem toSetItem(String targetUserId, ScoreRecordData record) {
        boolean targetInSideA = targetUserId.equals(record.getSideAPlayer1())
                || targetUserId.equals(record.getSideAPlayer2());
        boolean targetWon = (targetInSideA && "A".equals(record.getWinSide()))
                || (!targetInSideA && "B".equals(record.getWinSide()));
        ResultTypeEnum resultType = targetWon ? ResultTypeEnum.WIN : ResultTypeEnum.LOSE;

        return new MyProfileSetScoreDTO.SetItem()
                .setResultType(resultType)
                .setResultTypeShow(resultType.getShow())
                .setMatchType(record.getMatchType())
                .setMatchTypeShow(record.getMatchType().getName())
                .setSetFormat(record.getSetFormat())
                .setSetFormatShow(record.getSetFormat().getShow())
                .setDate(record.getMeetupDate().format(DATE_FORMATTER))
                .setSideAPlayer1UserId(record.getSideAPlayer1())
                .setSideAPlayer1AvatarUrl(QiniuConfiguration.buildSignedUrl(record.getSideAPlayer1Avatar()))
                .setSideAPlayer1Gender(record.getSideAPlayer1Gender())
                .setSideAPlayer2UserId(record.getSideAPlayer2())
                .setSideAPlayer2AvatarUrl(QiniuConfiguration.buildSignedUrl(record.getSideAPlayer2Avatar()))
                .setSideAPlayer2Gender(record.getSideAPlayer2Gender())
                .setSideAScore(String.valueOf(record.getSideAScore()))
                .setSideATiebreakScore(scoreToString(record.getSideATiebreakScore()))
                .setSideBPlayer1UserId(record.getSideBPlayer1())
                .setSideBPlayer1AvatarUrl(QiniuConfiguration.buildSignedUrl(record.getSideBPlayer1Avatar()))
                .setSideBPlayer1Gender(record.getSideBPlayer1Gender())
                .setSideBPlayer2UserId(record.getSideBPlayer2())
                .setSideBPlayer2AvatarUrl(QiniuConfiguration.buildSignedUrl(record.getSideBPlayer2Avatar()))
                .setSideBPlayer2Gender(record.getSideBPlayer2Gender())
                .setSideBScore(String.valueOf(record.getSideBScore()))
                .setSideBTiebreakScore(scoreToString(record.getSideBTiebreakScore()));
    }

    private String scoreToString(Integer score) {
        return score == null ? null : score.toString();
    }
}
