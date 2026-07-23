package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ResultConfirmCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    @NotNull(message = "确认状态不能为空")
    private Boolean confirm;

    private String rejectReason;

    private String rejectReasonText;

}
