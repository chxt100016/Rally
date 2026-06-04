package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 列表筛选+排序+分页入参
 */
@Data
public class MeetupListQuery {
    /** 城市编码（必传） */
    @NotBlank(message = "请选择城市")
    private String cityCode;

    /** 排序方式：time / distance */
    private String sort;

    /** 页码，默认1 */
    @Min(value = 1, message = "页码最小为1")
    private Integer pageNo = 1;

    /** 每页数量，默认10 */
    @Min(value = 1, message = "每页数量最小为1")
    private Integer pageSize = 10;

    /** 经度（距离排序必传） */
    private Double lng;

    /** 纬度（距离排序必传） */
    private Double lat;

    /** 半径（km） */
    private BigDecimal radiusKm;

    /** 水平最小值 */
    private BigDecimal levelMin;

    /** 水平最大值 */
    private BigDecimal levelMax;

    /** 约球类型 */
    private MatchTypeEnum matchType;

    /** 开始时间起 */
    private LocalDateTime startFrom;

    /** 开始时间止 */
    private LocalDateTime startTo;

    /** 标签 */
    private List<String> tags;
}
