package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtCollectModeEnum;
import lombok.Data;

import java.util.List;

/**
 * 球场抓取收录结果
 */
@Data
public class CourtCollectResultDTO {
    private String cityCode;
    private String cityName;
    private CourtCollectModeEnum mode;
    /** 本次实际检索的区县数量，未按区县拆分时为 0 */
    private Integer districtCount;
    /** 从地图服务取回的场所记录总条数 */
    private Integer fetchedCount;
    /** 被校验丢弃与被就近合并并掉的条数 */
    private Integer filteredCount;
    /** 解析出的球场条数 */
    private Integer validCount;
    private Integer insertedCount;
    private Integer updatedCount;
    private Integer skippedCount;
    /** 抓取失败的区县编码 */
    private List<String> failedDistricts;
}
