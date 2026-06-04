package com.rally.score.suggest;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.score.model.SuggestResult;
import com.rally.domain.user.gateway.TennisProfileGateway;
import com.rally.domain.user.model.TennisProfileData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 校准度建议触发判定服务
 * 触发条件：总票数 >= suggest_min_votes && 偏差 >= suggest_deviation && 同向票占比 >= suggest_concentration
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CalibrationSuggestService {

    private final TennisProfileGateway profileGateway;

    /**
     * 判定是否触发自评调整建议
     *
     * @param userId        用户 ID
     * @param totalVotes    有效票数
     * @param deviation     偏差幅度
     * @param direction     偏差方向（ABOVE/BELOW）
     * @param concentration 同向票占比
     * @return 建议结果
     */
    public SuggestResult evaluate(String userId, int totalVotes, double deviation,
                                   String direction, double concentration) {
        SuggestResult result = new SuggestResult();

        // 读取触发阈值
        int suggestMinVotes = SystemConfig.getInt("score.calibration.suggest_min_votes", 8);
        float suggestDeviation = SystemConfig.getFloat("score.calibration.suggest_deviation", 0.25f);
        float suggestConcentration = SystemConfig.getFloat("score.calibration.suggest_concentration", 0.60f);

        // 判定是否触发
        boolean triggered = totalVotes >= suggestMinVotes
                && deviation >= suggestDeviation
                && concentration >= suggestConcentration;

        result.setTriggered(triggered);
        result.setVoterCount(totalVotes);
        result.setDirection(direction);
        result.setConcentration(BigDecimal.valueOf(concentration));

        if (!triggered) {
            return result;
        }

        // 获取当前自评 NTRP
        BigDecimal currentNtrp = profileGateway.findByUserId(userId)
                .map(TennisProfileData::getNtrpScore)
                .orElse(null);

        if (currentNtrp == null) {
            result.setTriggered(false);
            return result;
        }

        result.setCurrentNtrp(currentNtrp);

        // 计算建议自评（MVP 步进 0.5 级，封顶 1.5~7.0）
        BigDecimal step = BigDecimal.valueOf(0.5);
        BigDecimal minNtrp = BigDecimal.valueOf(1.5);
        BigDecimal maxNtrp = BigDecimal.valueOf(7.0);

        BigDecimal suggestedNtrp;
        if ("ABOVE".equals(direction)) {
            // 偏高 → 建议下调
            suggestedNtrp = currentNtrp.subtract(step);
            if (suggestedNtrp.compareTo(minNtrp) < 0) {
                suggestedNtrp = minNtrp;
            }
        } else {
            // 偏低 → 建议上调
            suggestedNtrp = currentNtrp.add(step);
            if (suggestedNtrp.compareTo(maxNtrp) > 0) {
                suggestedNtrp = maxNtrp;
            }
        }

        result.setSuggestedNtrp(suggestedNtrp.setScale(1, RoundingMode.HALF_UP));

        return result;
    }
}
