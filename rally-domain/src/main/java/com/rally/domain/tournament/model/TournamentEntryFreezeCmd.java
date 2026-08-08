package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 运营冻结赛事报名入参。 */
@Data
public class TournamentEntryFreezeCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    /** 待冻结用户bizId */
    @NotBlank(message = "用户ID不能为空")
    private String userId;
}
