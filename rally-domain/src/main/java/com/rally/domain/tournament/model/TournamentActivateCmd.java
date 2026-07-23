package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 激活赛事入参
 */
@Data
public class TournamentActivateCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;
}
