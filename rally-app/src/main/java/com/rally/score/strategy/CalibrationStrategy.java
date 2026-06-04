package com.rally.score.strategy;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.review.gateway.ReviewGateway;
import com.rally.domain.review.model.ReviewData;
import com.rally.domain.score.enums.ScoreDimensionEnum;
import com.rally.domain.score.model.ScoreChange;
import com.rally.domain.score.model.ScoreContext;
import com.rally.domain.user.enums.ChangeReasonEnum;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 校准度策略（全量重算分）
 * 基于他人 NTRP 三元投票（higher/same/lower），档位制给分
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalibrationStrategy implements ScoreStrategy {

    private final ReviewGateway reviewGateway;
    private final TennisProfileGateway profileGateway;

    @Override
    public ScoreDimensionEnum dimension() {
        return ScoreDimensionEnum.CALIBRATION;
    }

    @Override
    public ScoreChange calculate(ScoreContext ctx, String userId) {
        ScoreChange change = new ScoreChange();
        change.setDimension(ScoreDimensionEnum.CALIBRATION);
        change.setRefId(ctx.getMeetupId());
        change.setReason(ChangeReasonEnum.SYSTEM);

        // 获取当前校准度
        BigDecimal before = profileGateway.findByUserId(userId)
                .map(TennisProfileData::getCalibrationScore)
                .orElse(BigDecimal.valueOf(80));
        change.setBefore(before);

        // 1. 拉取所有对该用户的 ntrp_vote 投票
        List<ReviewData> votes = reviewGateway.listByToUserAndType(userId, "ntrp_vote");

        // 2. 反滥用剔除（超出部分剔除，保留前 N=3 张 lower 票）
        int lowerVoteMax = SystemConfig.getInt("anti_abuse.lower_vote_max_per_target", 3);
        Map<String, Integer> lowerCountByFrom = new HashMap<>();
        int nHigher = 0, nSame = 0, nLower = 0;

        for (ReviewData vote : votes) {
            String value = vote.getReviewValue();
            String fromUserId = vote.getFromUserId();

            if ("lower".equalsIgnoreCase(value)) {
                // 统计该 from 对该 to 的 lower 票数
                int count = lowerCountByFrom.getOrDefault(fromUserId, 0) + 1;
                lowerCountByFrom.put(fromUserId, count);
                // 只有未超限时才计入
                if (count <= lowerVoteMax) {
                    nLower++;
                }
            } else if ("higher".equalsIgnoreCase(value)) {
                nHigher++;
            } else if ("same".equalsIgnoreCase(value)) {
                nSame++;
            }
        }

        int total = nHigher + nSame + nLower;

        // 3. 票数不足：给默认分 80
        int minVotes = SystemConfig.getInt("score.calibration.min_votes", 10);
        if (total < minVotes) {
            int insufficientScore = SystemConfig.getInt("score.calibration.score_insufficient", 80);
            BigDecimal after = BigDecimal.valueOf(insufficientScore);
            change.setAfter(after);
            change.setValue(after.subtract(before));
            return change;
        }

        // 4. 计算偏差比例
        double biasLow = (double) nLower / total;   // 认为你自评偏高
        double biasHigh = (double) nHigher / total;  // 认为你自评偏低
        double deviation = Math.max(biasLow, biasHigh);
        String direction = biasLow >= biasHigh ? "ABOVE" : "BELOW";

        // 5. 档位落分
        double t1 = SystemConfig.getFloat("score.calibration.deviation_t1", 0.20f);
        double t2 = SystemConfig.getFloat("score.calibration.deviation_t2", 0.50f);

        int score;
        if (deviation < t1) {
            score = SystemConfig.getInt("score.calibration.score_under_t1", 100);
        } else if (deviation < t2) {
            if ("BELOW".equals(direction)) {
                score = SystemConfig.getInt("score.calibration.score_below_t1_t2", 75);
            } else {
                score = SystemConfig.getInt("score.calibration.score_above_t1_t2", 50);
            }
        } else {
            if ("BELOW".equals(direction)) {
                score = SystemConfig.getInt("score.calibration.score_below_t2", 55);
            } else {
                score = SystemConfig.getInt("score.calibration.score_above_t2", 20);
            }
        }

        BigDecimal after = BigDecimal.valueOf(score);
        change.setAfter(after);
        change.setValue(after.subtract(before));
        change.setRemark("偏差=" + String.format("%.2f", deviation) + ", 方向=" + direction);

        return change;
    }
}
