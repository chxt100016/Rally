package com.rally.domain.court.model;

import com.rally.domain.court.enums.CourtEnvironmentEnum;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 从一条场所记录解析出的球场属性结论。
 */
@Data
public class CourtProfileResolveResult {
    /** 球场环境，判不出时为空 */
    private CourtEnvironmentEnum environment;
    /** 命中的球场标签，按判定顺序排列，未去重 */
    private List<String> tags = Collections.emptyList();
    private Double lng;
    private Double lat;
    /** 展示资料，入参缺哪项就不给哪项 */
    private CourtProfile profile;
}
