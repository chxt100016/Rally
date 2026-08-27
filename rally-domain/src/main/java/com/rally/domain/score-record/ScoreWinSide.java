package com.rally.domain.meetup.scorerecord;

/** 合法比分推导出的获胜方。 */
public enum ScoreWinSide {
    A,
    B;

    public String storageValue() {
        return name();
    }
}
