package com.rally.domain.score.model;

import lombok.Data;

/**
 * 一次 ELO 计算结果（单用户）
 */
@Data
public class EloResult {

    /** 用户 ID */
    private String userId;

    /** 变更前 ELO 分 */
    private float before;

    /** 变更后 ELO 分 */
    private float after;

    /** 变更量（delta = after - before） */
    private float delta;

    /** 本场计入的盘数 */
    private int matchCount;

    public EloResult(String userId, float before) {
        this.userId = userId;
        this.before = before;
        this.after = before;
    }

    /**
     * 应用 delta
     */
    public void applyDelta(float delta) {
        this.delta += delta;
        this.after = this.before + this.delta;
    }
}
