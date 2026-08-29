package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 运营整组淘汰指定赛事当前轮次中未进入比赛的参赛者。 */
@Data
public class TournamentUnmatchedEntryEliminationCmd {

    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;
}
