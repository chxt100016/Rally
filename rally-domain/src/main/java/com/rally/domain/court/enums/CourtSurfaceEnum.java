package com.rally.domain.court.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 球场表面材质
 */
@AllArgsConstructor
@Getter
public enum CourtSurfaceEnum {
    HARD("硬地"),
    CLAY("红土"),
    GRASS("草地");
    public final String label;
}
