package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import lombok.Data;

import java.util.List;

/**
 * 签表内单场比赛
 */
@Data
public class TournamentBracketMatchDTO {
    private String matchId;
    private Integer matchNo;
    private List<MatchOpponentDTO> participants;
    private String winnerId;
    private TournamentMatchStatusEnum status;
}
