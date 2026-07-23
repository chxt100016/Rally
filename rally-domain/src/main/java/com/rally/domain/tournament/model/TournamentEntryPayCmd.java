package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 支付报名费入参
 */
@Data
public class TournamentEntryPayCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;
}
