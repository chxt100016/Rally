package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SubmitResultCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    @NotNull(message = "获胜方不能为空")
    private Integer winnerEntryNo;

}
