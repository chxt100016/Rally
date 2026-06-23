package com.rally.tour.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetScore {
    private Long matchId;
    private Integer setNumber;
    private Integer p1Games;
    private Integer p2Games;
    private Integer p1Tiebreak;
    private Integer p2Tiebreak;
}
