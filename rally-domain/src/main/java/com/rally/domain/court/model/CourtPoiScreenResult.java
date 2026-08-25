package com.rally.domain.court.model;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 场所记录筛选与就近合并的结论。
 */
@Data
public class CourtPoiScreenResult {
    private List<CourtPoiCluster> clusters = Collections.emptyList();
    /** 未通过有效性判定被丢弃的条数 */
    private int rejectedCount;
    /** 通过判定但被并进其他记录的条数 */
    private int mergedCount;
}
