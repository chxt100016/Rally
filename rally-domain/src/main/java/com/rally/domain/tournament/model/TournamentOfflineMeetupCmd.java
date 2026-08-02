package com.rally.domain.tournament.model;

import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 创建赛事线下赛活动入参。人员、标题和赛事规则均由赛事配置自动确定。 */
@Data
public class TournamentOfflineMeetupCmd {

    @NotBlank(message = "赛事ID不能为空")
    private String tournamentId;

    @NotBlank(message = "活动创建人不能为空")
    private String creatorId;

    @NotNull(message = "请选择活动开始时间")
    private LocalDateTime startTime;

    @NotNull(message = "请选择持续时长")
    private BigDecimal duration;

    @Size(max = 128, message = "场地名称不超过128字符")
    private String courtName;

    @NotBlank(message = "请填写场地地址")
    @Size(max = 256, message = "场地地址不超过256字符")
    private String courtAddress;

    private CourtSelectModeEnum courtSelectMode;
    private String courtId;

    @NotBlank(message = "请选择城市")
    private String cityCode;
    private String districtCode;

    @NotNull(message = "请选择场地位置")
    @DecimalMin(value = "-180", message = "经度范围-180~180")
    @DecimalMax(value = "180", message = "经度范围-180~180")
    private Double courtLng;

    @NotNull(message = "请选择场地位置")
    @DecimalMin(value = "-90", message = "纬度范围-90~90")
    @DecimalMax(value = "90", message = "纬度范围-90~90")
    private Double courtLat;

    private String courtIndex;
}
