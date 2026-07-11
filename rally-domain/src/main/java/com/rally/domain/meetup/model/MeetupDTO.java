package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.rally.domain.meetup.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球基本信息 DTO（不含计算字段和关联信息）
 */
@Data
public class MeetupDTO {
    private String meetupId;
    private String title;
    private MatchTypeEnum matchType;
    private String matchTypeLabel;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private String cityCode;
    private String cityName;
    private String districtName;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
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
    private MeetupStatusEnum status;
    /** 场地索引，前端透传存储 */
    private String courtIndex;
    private LocalDateTime createTime;
}
