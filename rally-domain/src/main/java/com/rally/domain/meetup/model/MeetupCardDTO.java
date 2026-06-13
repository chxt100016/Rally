package com.rally.domain.meetup.model;

import com.alibaba.fastjson2.annotation.JSONField;
import com.rally.domain.meetup.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 约球列表卡片 DTO（纯数据，不含计算字段）
 */
@Data
public class MeetupCardDTO {
    private String meetupId;
    private String title;
    private MatchTypeEnum matchType;
    private Integer maxPlayers;
    private Integer currentPlayers;
    @JSONField(format = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    private BigDecimal duration;
    private String cityName;
    private String districtName;
    private String courtName;
    private Double courtLng;
    private Double courtLat;
    private LevelModeEnum levelMode;
    private BigDecimal levelMin;
    private BigDecimal levelMax;
    private MeetupStatusEnum status;
    /** 距离（米），距离排序时返回 */
    private Double distanceMeters;
    /** 主标签：OPEN 状态展示区域名，其余状态展示状态文案 */
    private String primaryLabel;

}
