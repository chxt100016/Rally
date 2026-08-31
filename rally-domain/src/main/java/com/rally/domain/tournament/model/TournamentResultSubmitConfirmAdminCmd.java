package com.rally.domain.tournament.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 运营按赛事编号和比赛序号指定获胜方，一次性代提交并代确认全部参与者赛果。 */
@Data
public class TournamentResultSubmitConfirmAdminCmd {

    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    @NotNull(message = "比赛序号不能为空")
    @Min(value = 1, message = "比赛序号必须为正整数")
    private Integer matchNo;

    @NotNull(message = "获胜方参赛编号不能为空")
    private Integer winnerEntryNo;
}
