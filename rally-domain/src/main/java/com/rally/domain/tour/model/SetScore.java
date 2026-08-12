package com.rally.domain.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A set score in player1/player2 orientation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetScore {
    private Integer setNumber;
    private Integer p1Games;
    private Integer p2Games;
    private Integer p1Tiebreak;
    private Integer p2Tiebreak;
}
