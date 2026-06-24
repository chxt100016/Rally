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
    /** 报名状态列表（用户维度查询时，过滤有效参与记录用） */
    private List<String> registrationStatuses;
    /** 页码 */
    private Integer pageNo;
    /** 每页数量 */
    private Integer pageSize;
    /** 上一页最后一条记录的 bizId（searchAfter 游标，用户 Tab 单字段游标用） */
    private String lastId;
    /** 上一页最后一条的开球时间（时间排序复合游标用） */
    private LocalDateTime lastStartTime;
    /** 上一页最后一条的 bizId（时间排序复合游标的 tie-breaker） */
    private String lastBizId;
    /** 查询限制条数（searchAfter: size + 1用于判断hasMore） */
    private Integer limit;
}
