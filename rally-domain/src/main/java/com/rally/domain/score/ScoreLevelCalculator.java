package com.rally.domain.score;

import com.rally.domain.config.gateway.ConfigGateway;
import com.rally.domain.score.model.ScoreResult;
import com.rally.domain.user.enums.RatingLevelEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 总分聚合计算器（纯函数式工具，非策略）
 * total = reputation × W1 + credibility × W2 + calibration × W3
 * 等级落档：S/A/B/C
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreLevelCalculator {

    private final ConfigGateway config;

    /**
     * 聚合总分 + 等级落档
     */
    public RatingLevelEnum aggregate(BigDecimal reputation, BigDecimal credibility, BigDecimal calibration) {
        // 读取权重
        float w1 = config.getFloat("score.weights.reputation", 0.5f);
        float w2 = config.getFloat("score.weights.credibility", 0.3f);
        float w3 = config.getFloat("score.weights.calibration", 0.2f);

        // 三维分（缺失则用默认值）
        BigDecimal rep = reputation != null ? reputation : BigDecimal.valueOf(100);
        BigDecimal cred = credibility != null ? credibility : BigDecimal.ZERO;
        BigDecimal cal = calibration != null ? calibration : BigDecimal.valueOf(80);

        // 计算总分
        float total = rep.floatValue() * w1 + cred.floatValue() * w2 + cal.floatValue() * w3;

        // 等级落档
        int sThreshold = config.getInt("score.rating.s_threshold", 90);
        int aThreshold = config.getInt("score.rating.a_threshold", 75);
        int bThreshold = config.getInt("score.rating.b_threshold", 55);

        RatingLevelEnum level;
        if (total >= sThreshold) {
            level = RatingLevelEnum.S;
        } else if (total >= aThreshold) {
            level = RatingLevelEnum.A;
        } else if (total >= bThreshold) {
            level = RatingLevelEnum.B;
        } else {
            level = RatingLevelEnum.C;
        }

        return level;
    }
}
