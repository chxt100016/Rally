package com.rally.domain.score;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.enums.RatingLevelEnum;
import com.rally.domain.user.model.TennisProfileData;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

/**
 * 总分聚合计算器（纯函数式工具，非策略）
 * total = reputation × W1 + credibility × W2 + calibration × W3
 * 等级落档：S/A/B/C
 */
@Slf4j
public class ProfileLevelCalculator {

    /**
     * 聚合总分 + 等级落档
     */
    public static String calculate(TennisProfileData profileData) {
        BigDecimal reputation = profileData.getReputationScore();
        BigDecimal credibility = profileData.getCredibilityScore();
        BigDecimal calibration = profileData.getCalibrationScore();

        // 读取权重
        float w1 = SystemConfig.getFloat("score.weights.reputation", 0.5f);
        float w2 = SystemConfig.getFloat("score.weights.credibility", 0.3f);
        float w3 = SystemConfig.getFloat("score.weights.calibration", 0.2f);

        // 三维分（缺失则用默认值）
        BigDecimal rep = reputation != null ? reputation : BigDecimal.valueOf(100);
        BigDecimal cred = credibility != null ? credibility : BigDecimal.ZERO;
        BigDecimal cal = calibration != null ? calibration : BigDecimal.valueOf(80);

        // 计算总分
        float total = rep.floatValue() * w1 + cred.floatValue() * w2 + cal.floatValue() * w3;

        // 等级落档
        int sThreshold = SystemConfig.getInt("score.rating.s_threshold", 90);
        int aThreshold = SystemConfig.getInt("score.rating.a_threshold", 75);
        int bThreshold = SystemConfig.getInt("score.rating.b_threshold", 55);

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

        return level.name();
    }
}
