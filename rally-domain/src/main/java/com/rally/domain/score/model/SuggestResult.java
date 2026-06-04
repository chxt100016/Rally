package com.rally.domain.score.model;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 校准度建议结果（供通知域推送）
 */
@Data
public class SuggestResult {

    /** 是否触发建议 */
    private boolean triggered;

    /** 用户当前自评 NTRP */
    private BigDecimal currentNtrp;

    /** 建议自评 NTRP */
    private BigDecimal suggestedNtrp;

    /** 有效投票数 */
    private int voterCount;

    /** 偏差方向：ABOVE(偏高) / BELOW(偏低) */
    private String direction;

    /** 同向票占比 */
    private BigDecimal concentration;
}
