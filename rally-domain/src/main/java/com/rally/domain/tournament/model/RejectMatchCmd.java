package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.ScheduleRejectReasonEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/** 各赛约等待场景共用的拒绝比赛请求。 */
@Data
public class RejectMatchCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    @NotNull(message = "拒绝理由不能为空")
    private ScheduleRejectReasonEnum rejectReason;
}
