package com.rally.domain.system.model;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 行政区划名录查询出参。名录查不到时 hit 为 false，本身不抛异常。
 */
@Data
public class LocationResult {
    /** 编码是否在名录中命中；DISTRICTS_OF_CITY 时表示城市编码本身是否命中 */
    private boolean hit;
    /** 命中时该编码对应的中文名称 */
    private String name;
    /** 仅 DISTRICTS_OF_CITY 时给出，该城市下属区县；没有下属区县时为空列表 */
    private List<Location> districts = Collections.emptyList();

    public static LocationResult miss() {
        return new LocationResult();
    }

    public static LocationResult of(String name) {
        LocationResult result = new LocationResult();
        result.setHit(true);
        result.setName(name);
        return result;
    }

    public static LocationResult of(String name, List<Location> districts) {
        LocationResult result = of(name);
        result.setDistricts(districts == null ? Collections.emptyList() : districts);
        return result;
    }
}
