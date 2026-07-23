package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentStatusEnum;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 后台赛事列表查询入参
 */
@Data
public class TournamentAdminListCmd {

    /** 城市编码，选填 */
    private String cityCode;

    /** 状态，选填 */
    private TournamentStatusEnum status;

    /** NTRP等级，选填 */
    private String ntrpLevel;

    /** 页码，从1开始 */
    @NotNull(message = "请填写页码")
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNum;

    /** 每页条数 */
    @NotNull(message = "请填写每页条数")
    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    private Integer pageSize;
}
