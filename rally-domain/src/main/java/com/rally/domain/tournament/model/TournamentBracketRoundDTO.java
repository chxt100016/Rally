package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.util.List;

/**
 * 签表内单轮次
 */
@Data
public class TournamentBracketRoundDTO {
    private TournamentRoundEnum round;
    private List<TournamentBracketMatchDTO> matches;
}
