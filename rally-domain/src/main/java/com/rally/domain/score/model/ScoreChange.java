package com.rally.domain.score.model;

import com.rally.domain.score.enums.ScoreDimensionEnum;
import com.rally.domain.user.enums.ChangeReasonEnum;
import lombok.Data;

/**
 * 一条分数变更明细
 */
@Data
public class ScoreChange {

    /** 维度 */
    private ScoreDimensionEnum dimension;

    /** 变更前分值 */
    private Integer before;

    /** 变更后分值 */
    private Integer after;

    /** 变更量（delta = after - before） */
    private Integer value;

    /** 变更原因 */
    private ChangeReasonEnum reason;

    /** 关联业务 biz_id（如 meetupId） */
    private String refId;

    /** 备注说明 */
    private String remark;

    /**
     * 是否有实际变更
     */
    public boolean hasChanged() {
        return before != null && after != null && !before.equals(after);
    }
}
