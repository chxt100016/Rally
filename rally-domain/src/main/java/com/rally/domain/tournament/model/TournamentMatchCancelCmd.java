package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** 运营批量取消一个赛事中尚未提交订场信息的比赛。 */
@Data
public class TournamentMatchCancelCmd {
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;
}
