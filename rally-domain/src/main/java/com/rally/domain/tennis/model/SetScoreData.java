package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 盘分数据模型（domain 层）
 */
@Data
public class SetScoreData {

    /** 对应 tennis_match.id，与 MatchData.tennisMatchId 关联 */
    private Long tennisMatchId;
    private Integer setNumber;
    private Integer p1Games;
    private Integer p2Games;
    private Integer p1Tiebreak;
    private Integer p2Tiebreak;
}
