package com.rally.domain.score;

import com.rally.domain.system.SystemConfig;
import com.rally.domain.user.enums.RatingLevelEnum;
import com.rally.domain.user.model.ScoreItemDTO;
import com.rally.domain.user.model.TennisProfileData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 总分聚合计算器（纯函数式工具，非策略）
 * total = reputation × W1 + credibility × W2 + calibration × W3
 * 等级落档：S/A/B/C
 */
@Slf4j
public class ScoreLevelCalculator {

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

    /**
     * 构建评分明细列表
     * 包含信誉分、可信度、校准度三项
     */
    public static List<ScoreItemDTO> buildScoreItems(TennisProfileData profileData) {
        List<ScoreItemDTO> items = new ArrayList<>();

        // 信誉分
        ScoreItemDTO reputationItem = new ScoreItemDTO();
        reputationItem.setName("信誉分");
        reputationItem.setKey("reputation_score");
        reputationItem.setValue(profileData != null && profileData.getReputationScore() != null ?
                profileData.getReputationScore().toPlainString() : "0");
        reputationItem.setLabel("权重" + SystemConfig.getInt("score.weight.reputation", 50) + "%");
        reputationItem.setInfo(SystemConfig.getString("score.info.reputation", "信誉分说明你有信誉"));
        reputationItem.setSort("");
        items.add(reputationItem);

        // 可信度
        ScoreItemDTO credibilityItem = new ScoreItemDTO();
        credibilityItem.setName("可信度");
        credibilityItem.setKey("credibility_score");
        credibilityItem.setValue(profileData != null && profileData.getCredibilityScore() != null ?
                profileData.getCredibilityScore().toPlainString() : "0");
        credibilityItem.setLabel("权重" + SystemConfig.getInt("score.weight.credibility", 30) + "%");
        credibilityItem.setInfo(SystemConfig.getString("score.info.credibility", "可信度说明你可信"));
        credibilityItem.setSort("");
        items.add(credibilityItem);

        // 校准度
        ScoreItemDTO calibrationItem = new ScoreItemDTO();
        calibrationItem.setName("校准度");
        calibrationItem.setKey("calibration_score");
        calibrationItem.setValue(profileData != null && profileData.getCalibrationScore() != null ?
                profileData.getCalibrationScore().toPlainString() : "0");
        calibrationItem.setLabel("权重" + SystemConfig.getInt("score.weight.calibration", 20) + "%");
        calibrationItem.setInfo(SystemConfig.getString("score.info.calibration", "校准度说明你校准"));
        calibrationItem.setSort("");
        items.add(calibrationItem);

        return items;
    }
}
