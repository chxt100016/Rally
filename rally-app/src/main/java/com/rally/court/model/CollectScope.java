package com.rally.court.model;

import lombok.Data;

import java.util.List;

/**
 * 本次抓取的检索范围，resolve-collect-scope 活动的产出。
 */
@Data
public class CollectScope {
    private String cityCode;
    private String cityName;
    /** 要逐个检索的行政区划编码；推不出区县时只含城市编码本身 */
    private List<String> regionCodes;
    /** 推导出的区县数量，未按区县拆分时为 0 */
    private int districtCount;
}
