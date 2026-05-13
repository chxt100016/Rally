package com.rally.domain.tennis.model;

import lombok.Data;

/**
 * 盘分信息 VO
 */
@Data
public class SetScoreVO {

    private Integer number;
    private Integer player1;
    private Integer player2;
    private Integer tiebreak1;
    private Integer tiebreak2;
}
