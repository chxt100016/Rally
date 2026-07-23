package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SelectCourtBookerCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

}
