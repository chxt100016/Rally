package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 盘分数据模型（domain 层）
 */
@Data
public class SetScoreData {

    private String matchId;
    private Integer setNumber;
    private Integer p1Games;
    private Integer p2Games;
    private Integer p1Tiebreak;
    private Integer p2Tiebreak;
}
