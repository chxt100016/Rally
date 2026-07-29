package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class SubmitResultCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    @NotEmpty(message = "获胜方不能为空")
    private List<Integer> winnerEntryNos;

    /** 本次微信订阅授权成功的赛事通知场景 */
    private List<String> acceptedNoticeScenes;
}
