package com.rally.domain.court.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 就近合并后的一簇场所记录：一条主记录与其余被并掉的记录。
 */
@Data
@AllArgsConstructor
public class CourtPoiCluster {
    /** 簇内评分最高的一条，作为球场主记录 */
    private CourtPoi keeper;
    /** 被并掉的其余记录，名称将作为球场别名 */
    private List<CourtPoi> merged;
}
