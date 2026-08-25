package com.rally.court.model;

import com.rally.domain.court.model.CourtPoi;
import lombok.Data;

import java.util.List;

/**
 * 从地图服务取回的场所记录，fetch-amap-courts 活动的产出。
 */
@Data
public class FetchedPois {
    private List<CourtPoi> pois;
    private int fetchedCount;
    /** 检索失败的行政区划编码 */
    private List<String> failedRegions;
}
