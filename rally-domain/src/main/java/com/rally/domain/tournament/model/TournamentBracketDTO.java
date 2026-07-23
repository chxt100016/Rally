package com.rally.domain.tournament.model;

import lombok.Data;

import java.util.List;

/**
 * 签表对阵图数据
 */
@Data
public class TournamentBracketDTO {
    private List<TournamentBracketRoundDTO> rounds;
}
