package com.rally.domain.score;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.system.enums.SystemConfigKey;
import com.rally.domain.user.enums.RatingLevelEnum;
import com.rally.domain.user.model.TennisProfileData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;


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
        Integer reputation = profileData.getReputationScore();
        Integer credibility = profileData.getCredibilityScore();
        Integer calibration = profileData.getCalibrationScore();

        // 读取权重
        float w1 = SystemConfig.getFloat(SystemConfigKey.SCORE_WEIGHTS_REPUTATION.getKey());
        float w2 = SystemConfig.getFloat(SystemConfigKey.SCORE_WEIGHTS_CREDIBILITY.getKey());
        float w3 = SystemConfig.getFloat(SystemConfigKey.SCORE_WEIGHTS_CALIBRATION.getKey());



        // 计算总分
        float total = reputation * w1 + credibility * w2 + calibration * w3;

        // 等级落 档
        int sThreshold = SystemConfig.getInt(SystemConfigKey.SCORE_RATING_S_THRESHOLD.getKey());
        int aThreshold = SystemConfig.getInt(SystemConfigKey.SCORE_RATING_A_THRESHOLD.getKey());
        int bThreshold = SystemConfig.getInt(SystemConfigKey.SCORE_RATING_B_THRESHOLD.getKey());


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
