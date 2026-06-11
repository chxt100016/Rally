package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球领域数据对象（含 lng/lat 解构，跨层传递）
 */
@Data
public class MeetupData {
    private String bizId;
    private String creatorId;
    private String title;
    private MatchTypeEnum matchType;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private String cityCode;
    private String cityName;
    private String districtName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal duration;
    private String courtName;
    private String courtAddress;
    private Double courtLng;
    private Double courtLat;

    private LevelModeEnum levelMode;
    private BigDecimal levelMin;
    private BigDecimal levelMax;
    private GenderLimitEnum genderLimit;
    private JoinModeEnum joinMode;
    private List<CostItem> costItems;
    private MeetupStatusEnum status;
    /** 场地索引，前端透传存储 */
    private String courtIndex;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
