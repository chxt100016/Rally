package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 解冻本人赛事报名入参
 */
@Data
public class TournamentEntryUnfreezeCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;
}
