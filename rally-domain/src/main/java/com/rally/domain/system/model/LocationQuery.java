package com.rally.domain.system.model;

import com.rally.domain.system.enums.LocationScopeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 行政区划名录查询入参
 */
@Data
@AllArgsConstructor
public class LocationQuery {
    /** 六位行政区划编码 */
    private String code;
    private LocationScopeEnum scope;
}
