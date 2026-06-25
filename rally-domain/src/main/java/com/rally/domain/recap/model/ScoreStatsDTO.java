package com.rally.domain.recap.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScoreStatsDTO {

    private Long total;
    private Long wins;
    private Long losses;
    private String winRate;
    private String streakType;
    private Long streakCount;
}
