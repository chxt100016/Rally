package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.RebookReasonEnum;
import com.rally.domain.tournament.enums.ScheduleRejectReasonEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ScheduleConfirmCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    @NotNull(message = "确认状态不能为空")
    private Boolean confirm;

    private ScheduleRejectReasonEnum rejectReason;

    private RebookReasonEnum rebookReason;

}
