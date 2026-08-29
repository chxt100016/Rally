package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 运营淘汰指定赛事中当前轮次未进入比赛的单个用户。 */
@Data
public class TournamentUnmatchedEntryEliminationCmd {

    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
