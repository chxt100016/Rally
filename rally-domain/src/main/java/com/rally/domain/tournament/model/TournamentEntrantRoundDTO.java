package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.util.List;

/**
 * 单轮次参赛者列表。
 */
@Data
public class TournamentEntrantRoundDTO {
    private TournamentRoundEnum round;
    private String roundShow;
    private List<TournamentEntrantDTO> entrants;
}
