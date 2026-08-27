package com.rally.domain.tour.draw;

/** 状态由两个独立可空的结构字段派生，不单独落库。 */
public enum TourDrawStatus {
    PLACEHOLDER,
    PARTIAL,
    STRUCTURED
}
