package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.LevelModeEnum;
import com.rally.domain.meetup.enums.MatchTypeEnum;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球列表查询参数（数据库查询用）
 */
@Data
@Builder
public class MeetupListQueryParam {
    /** 城市编码 */
    private String cityCode;
    /** 约球类型 */
    private MatchTypeEnum matchType;
    /** 开始时间起 */
    private LocalDateTime startTimeFrom;
    /** 开始时间止 */
    private LocalDateTime startTimeTo;
    /** 水平模式 */
    private LevelModeEnum levelMode;
    /** 水平最小值 */
    private BigDecimal levelMin;
    /** 水平最大值 */
    private BigDecimal levelMax;
    /** 约球ID列表（距离筛选时使用） */
    private List<String> meetupIds;
    /** 创建人 ID（我发布 tab 用） */
    private String creatorId;
    /** 状态列表筛选（进行中/已完成 tab 用） */
    private List<String> statusList;
    /** 当前用户 ID（参与人关联查询用） */
    private String userId;
    /** 页码 */
    private Integer pageNo;
    /** 每页数量 */
    private Integer pageSize;
}
