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

    /** 球场选择模式：TEXT(文本搜索) / MAP(地图选择) / FREE(自由输入) */
    private String courtSelectMode;

    /** 球场库ID，TEXT/MAP模式下从球场库选中时传入 */
    private String courtId;

    /** 城市编码 */
    @NotBlank(message = "请选择城市")
    private String cityCode;

    /** 区县编码，选填 */
    private String districtCode;

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

    /** 水平最小值，RANGE/EXACT/ABOVE 必填 */
    @DecimalMin(value = "1.5", message = "水平最小值范围为1.5~7.0")
    @DecimalMax(value = "7.0", message = "水平最小值范围为1.5~7.0")
    private BigDecimal levelMin;

    /** 水平最大值，RANGE/EXACT/BELOW 必填 */
    @DecimalMin(value = "1.5", message = "水平最大值范围为1.5~7.0")
    @DecimalMax(value = "7.0", message = "水平最大值范围为1.5~7.0")
    private BigDecimal levelMax;

    /** 性别限制 */
    @NotNull(message = "请选择性别限制")
    private GenderLimitEnum genderLimit;

    /** 加入模式 */
    @NotNull(message = "请选择加入模式")
    private JoinModeEnum joinMode;

    /** 费用明细 */
    private List<CostItem> costItems;

    /** 场地索引，前端透传存储 */
    private String courtIndex;

    /** 本次微信订阅授权成功的通知场景（NoticeScene name 列表，需审批活动可含 PENDING_APPROVAL） */
    private List<String> acceptedNoticeScenes;
}
