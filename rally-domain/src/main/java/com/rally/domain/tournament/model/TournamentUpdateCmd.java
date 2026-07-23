package com.rally.domain.tournament.model;

import jakarta.validation.constraints.NotBlank;

/**
 * 编辑赛事草稿入参：bizId + 可改配置（复用创建入参的全部字段与校验）
 */
public class TournamentUpdateCmd extends TournamentCreateCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    public String getTournamentId() {
        return tournamentId;
    }

    public void setTournamentId(String tournamentId) {
        this.tournamentId = tournamentId;
    }
}
