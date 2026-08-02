package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.ResultRejectReasonEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ResultConfirmCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    @NotNull(message = "确认状态不能为空")
    private Boolean confirm;

    private ResultRejectReasonEnum rejectReason;

    /** 本次微信订阅授权成功的赛事通知场景 */
    private List<String> acceptedNoticeScenes;
}
