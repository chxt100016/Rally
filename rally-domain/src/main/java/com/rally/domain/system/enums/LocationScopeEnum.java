package com.rally.domain.system.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 行政区划名录查询范围
 */
@AllArgsConstructor
@Getter
public enum LocationScopeEnum {
    CITY("查城市名称"),
    DISTRICT("查区县名称"),
    DISTRICTS_OF_CITY("推导城市下属区县");

    public final String label;
}
