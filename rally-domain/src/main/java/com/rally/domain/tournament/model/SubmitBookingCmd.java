package com.rally.domain.tournament.model;

import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SubmitBookingCmd {

    @NotBlank(message = "比赛ID不能为空")
    private String matchId;

    /** 场地名称，FREE模式手填 */
    private String courtName;

    /** 场地详细地址，FREE模式手填或TEXT/MAP模式选中后由球场库回填 */
    @NotBlank(message = "请填写场地地址")
    private String courtAddress;

    /** 球场选择模式：TEXT(文本搜索) / MAP(地图选择) / FREE(自由输入)，与约球域保持一致 */
    private CourtSelectModeEnum courtSelectMode;

    /** 球场库ID，TEXT/MAP模式下从球场库选中时传入 */
    private String courtId;

    /** 经度，FREE模式下由前端定位传入 */
    private Double courtLng;

    /** 纬度，FREE模式下由前端定位传入 */
    private Double courtLat;

    /** 城市编码，FREE模式下由前端传入，TEXT/MAP模式以球场库数据为准 */
    private String cityCode;

    @NotNull(message = "开始时间不能为空")
    private LocalDateTime scheduledStartTime;

    @NotNull(message = "时长不能为空")
    @Min(value = 1, message = "时长至少为1小时")
    private Integer scheduledDuration;

}
