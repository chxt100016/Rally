package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 发布约球入参
 */
@Data
public class MeetupPublishCmd {

    /** 标题，选填；不填后端按模板生成 */
    @Size(max = 128, message = "标题不超过128字符")
    private String title;

    /** 类型：单打/双打/拉球 */
    @NotNull(message = "请选择约球类型")
    private MatchTypeEnum matchType;

    /** 人数上限 */
    @NotNull(message = "请填写人数上限")
    @Min(value = 2, message = "人数上限至少2人")
    @Max(value = 16, message = "人数上限不超过16人")
    private Integer maxPlayers;

    /** 活动开始时间 */
    @NotNull(message = "请选择开始时间")
    private LocalDateTime startTime;

    /** 持续小时 */
    @NotNull(message = "请选择持续时长")
    private BigDecimal duration;

    /** 场地名称 */
    @Size(max = 128, message = "场地名称不超过128字符")
    private String courtName;

    /** 场地详细地址 */
    @NotBlank(message = "请填写场地地址")
    @Size(max = 256, message = "场地地址不超过256字符")
    private String courtAddress;

    /** 城市编码 */
    @NotBlank(message = "请选择城市")
    private String cityCode;

    /** 城市名称 */
    @NotBlank(message = "城市名称不能为空")
    private String cityName;

    /** 区域名称，选填 */
    private String districtName;

    /** 经度 */
    @NotNull(message = "请选择场地位置")
    @DecimalMin(value = "-180", message = "经度范围-180~180")
    @DecimalMax(value = "180", message = "经度范围-180~180")
    private Double courtLng;

    /** 纬度 */
    @NotNull(message = "请选择场地位置")
    @DecimalMin(value = "-90", message = "纬度范围-90~90")
    @DecimalMax(value = "90", message = "纬度范围-90~90")
    private Double courtLat;

    /** 水平要求模式 */
    @NotNull(message = "请填写水平限制类型")
    private LevelModeEnum levelMode;

    /** 水平值；RANGE 模式格式 "min:max"，其余模式为单值 */
    @NotBlank(message = "请填写水平值")
    private String levelValue;

    /** 性别限制 */
    @NotNull(message = "请选择性别限制")
    private GenderLimitEnum genderLimit;

    /** 加入模式 */
    @NotNull(message = "请选择加入模式")
    private JoinModeEnum joinMode;

    /** 费用明细 */
    private List<CostItem> costItems;
}
