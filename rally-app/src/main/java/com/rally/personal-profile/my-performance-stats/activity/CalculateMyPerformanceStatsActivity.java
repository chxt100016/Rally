package com.rally.personalprofile.myperformancestats.activity;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.recap.model.ScoreRecordData;
import com.rally.domain.recap.model.ScoreStatsDTO;
import com.rally.domain.recap.service.ScoreDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 业务活动 calculate-my-performance-stats：计算当前用户的盘级战绩。
 */
@Component
@RequiredArgsConstructor
public class CalculateMyPerformanceStatsActivity {

    private final ScoreDomainService scoreDomainService;

    public ScoreStatsDTO execute(String userId, MatchTypeEnum matchType) {
        // A1 保留原查询的四个球员位置匹配及 biz_id 倒序语义。
        List<ScoreRecordData> all = scoreDomainService.listScoresByUserId(userId);

        // A2 未指定类型时保留全部，指定时按持久化枚举精确匹配。
        List<ScoreRecordData> filtered = matchType == null
                ? all
                : all.stream().filter(record -> record.getMatchType() == matchType).toList();

        // A3 本人出现在 A 侧任一位置即按 A 侧判定，否则按 B 侧判定。
        long wins = filtered.stream().filter(record -> isWin(record, userId)).count();
        long total = filtered.size();

        return new ScoreStatsDTO()
                .setTotal(total)
                .setWins(wins)
                .setLosses(total - wins)
                .setWinRate(formatRate(wins, total))
                .setStreakType(computeStreakType(filtered, userId))
                .setStreakCount(computeStreakCount(filtered, userId));
    }

    private boolean isWin(ScoreRecordData record, String userId) {
        boolean userInSideA = userId.equals(record.getSideAPlayer1())
                || userId.equals(record.getSideAPlayer2());
        return (userInSideA && "A".equals(record.getWinSide()))
                || (!userInSideA && "B".equals(record.getWinSide()));
    }

    private String formatRate(long wins, long total) {
        if (total == 0) {
            return "--";
        }
        return String.format("%.1f", wins * 100.0 / total);
    }

    /** A4 从 biz_id 最大的首条记录取当前连续战绩类型。 */
    private String computeStreakType(List<ScoreRecordData> records, String userId) {
        if (records.isEmpty()) {
            return null;
        }
        return isWin(records.get(0), userId) ? "WIN" : "LOSE";
    }

    /** A4 遇到首条不同胜负结果即停止累计。 */
    private Long computeStreakCount(List<ScoreRecordData> records, String userId) {
        if (records.isEmpty()) {
            return null;
        }
        boolean firstWin = isWin(records.get(0), userId);
        long count = 0;
        for (ScoreRecordData record : records) {
            if (isWin(record, userId) != firstWin) {
                break;
            }
            count++;
        }
        return count;
    }
}
