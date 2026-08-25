package com.rally.domain.court.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 球场地理归属值对象。城市与区域的编码与名称由调用方按名录解析后传入。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourtLocation {
    private String cityCode;
    private String cityName;
    private String districtCode;
    private String districtName;
}
