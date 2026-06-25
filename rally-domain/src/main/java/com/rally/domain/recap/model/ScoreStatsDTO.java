package com.rally.domain.recap.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ScoreStatsDTO {

    private Long total;
    private Long singleCount;
    private Long doubleCount;
    private String winRate;
    private String singleWinRate;
    private String doubleWinRate;
    private String streakType;
    private Long streakCount;
}
