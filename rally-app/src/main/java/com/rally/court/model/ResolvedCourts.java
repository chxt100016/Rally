package com.rally.court.model;

import com.rally.domain.court.model.CourtCollectCmd;
import lombok.Data;

import java.util.List;

/**
 * 解析好的待写入球场资料，resolve-court-profile 活动的产出。
 */
@Data
public class ResolvedCourts {
    private List<CourtCollectCmd> courts;
    private int validCount;
}
