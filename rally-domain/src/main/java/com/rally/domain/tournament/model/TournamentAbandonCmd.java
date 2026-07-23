package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 废弃赛事入参
 */
@Data
public class TournamentAbandonCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    /** 废弃原因，选填 */
    private String reason;
}
