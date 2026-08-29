package com.rally.domain.tournament.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 运营终止指定赛事中一场未完成的比赛。 */
@Data
public class TournamentSingleMatchCancelCmd {

    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    @NotNull(message = "比赛序号不能为空")
    @Min(value = 1, message = "比赛序号必须为正整数")
    private Integer matchNo;
}
