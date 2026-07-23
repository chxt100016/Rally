package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.CourtAbilityEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 报名入参
 */
@Data
public class TournamentJoinCmd {

    /** 赛事bizId */
    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    /** 双打搭档用户ID，选填 */
    private String partnerId;

    /** 活动区域 */
    private List<String> preferredDistricts;

    /** 场地能力 */
    @NotNull(message = "请选择场地能力")
    private CourtAbilityEnum courtAbility;

    /** 可比赛时间 */
    private List<String> availableTimes;
}
