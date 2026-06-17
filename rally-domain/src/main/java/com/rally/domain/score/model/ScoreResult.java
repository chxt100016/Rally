package com.rally.domain.score.model;

import com.rally.domain.score.enums.ScoreDimensionEnum;
import com.rally.domain.user.enums.RatingLevelEnum;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单用户一次计算结果
 */
@Data
public class ScoreResult {

    /** 用户 ID */
    private String userId;

    /** 信誉分 */
    private Integer reputation;

    /** 可信度 */
    private Integer credibility;

    /** 校准度 */
    private Integer calibration;

    /** 总分（三维加权） */
    private Integer total;

    /** 球友评级 */
    private RatingLevelEnum ratingLevel;

    /** 是否新人 */
    private Boolean isNewbie;

    /** 各维度变更明细 */
    private List<ScoreChange> changes = new ArrayList<>();

    public ScoreResult(String userId) {
        this.userId = userId;
    }

    /**
     * 添加一条变更明细
     */
    public void put(ScoreChange change) {
        if (change == null) {
            return;
        }
        changes.add(change);
        // 更新对应维度的分值
        switch (change.getDimension()) {
            case REPUTATION -> this.reputation = change.getAfter();
            case CREDIBILITY -> this.credibility = change.getAfter();
            case CALIBRATION -> this.calibration = change.getAfter();
            default -> {}
        }
    }

    /**
     * 获取有实际变更的维度列表
     */
    public List<ScoreChange> changedDimensions() {
        return changes.stream().filter(ScoreChange::hasChanged).toList();
    }
}
