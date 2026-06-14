package com.rally.domain.score;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.RatingLevelEnum;
import com.rally.domain.user.model.TennisProfileData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

/**
 * 总分聚合计算器（纯函数式工具，非策略）
 * total = reputation × W1 + credibility × W2 + calibration × W3
 * 等级落档：S/A/B/C
 */
@Slf4j
public class ProfileLevelManager {

    /**
     * 聚合总分 + 等级落档
     */
    public static String calculate(TennisProfileData profileData) {
        if (profileData == null) {
            return StringUtils.EMPTY;
        }
        BigDecimal reputation = profileData.getReputationScore();
        BigDecimal credibility = profileData.getCredibilityScore();
        BigDecimal calibration = profileData.getCalibrationScore();

        // 读取权重
        float w1 = SystemConfig.getFloat(SystemConfigKey.SCORE_WEIGHTS_REPUTATION.getKey(), Float.parseFloat(SystemConfigKey.SCORE_WEIGHTS_REPUTATION.getDefaultValue()));
        float w2 = SystemConfig.getFloat(SystemConfigKey.SCORE_WEIGHTS_CREDIBILITY.getKey(), Float.parseFloat(SystemConfigKey.SCORE_WEIGHTS_CREDIBILITY.getDefaultValue()));
        float w3 = SystemConfig.getFloat(SystemConfigKey.SCORE_WEIGHTS_CALIBRATION.getKey(), Float.parseFloat(SystemConfigKey.SCORE_WEIGHTS_CALIBRATION.getDefaultValue()));

        // 三维分（缺失则用默认值）
        BigDecimal rep = reputation != null ? reputation : BigDecimal.valueOf(100);
        BigDecimal cred = credibility != null ? credibility : BigDecimal.ZERO;
        BigDecimal cal = calibration != null ? calibration : BigDecimal.valueOf(80);

        // 计算总分
        float total = rep.floatValue() * w1 + cred.floatValue() * w2 + cal.floatValue() * w3;

        // 等级落档
        int sThreshold = SystemConfig.getInt(SystemConfigKey.SCORE_RATING_S_THRESHOLD.getKey(), Integer.parseInt(SystemConfigKey.SCORE_RATING_S_THRESHOLD.getDefaultValue()));
        int aThreshold = SystemConfig.getInt(SystemConfigKey.SCORE_RATING_A_THRESHOLD.getKey(), Integer.parseInt(SystemConfigKey.SCORE_RATING_A_THRESHOLD.getDefaultValue()));
        int bThreshold = SystemConfig.getInt(SystemConfigKey.SCORE_RATING_B_THRESHOLD.getKey(), Integer.parseInt(SystemConfigKey.SCORE_RATING_B_THRESHOLD.getDefaultValue()));

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
