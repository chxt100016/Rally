package com.rally.domain.tour.model;

import lombok.Data;

/**
 * 盘分数据模型（domain 层）
 */
@Data
public class SetScoreData {

    /** 对应 tour_match.id，与 MatchData.tourMatchId 关联 */
    private Long tourMatchId;
    private Integer setNumber;
    private Integer p1Games;
    private Integer p2Games;
    private Integer p1Tiebreak;
    private Integer p2Tiebreak;
}
