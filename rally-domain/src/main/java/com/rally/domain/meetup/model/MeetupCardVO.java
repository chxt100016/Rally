package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 列表卡片精简视图
 */
@Data
public class MeetupCardVO {
    private String meetupId;
    private String title;
    private MatchTypeEnum matchType;
    private Integer maxPlayers;
    private Integer currentPlayers;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal duration;
    private String courtName;
    private String courtAddress;
    private LevelModeEnum levelMode;
    private String levelValue;
    private GenderLimitEnum genderLimit;
    private JoinModeEnum joinMode;
    private MeetupStatusEnum status;

    // 计算字段
    /** 每人费用（分） */
    private Integer perPersonCost;
    /** 距离（米） */
    private Double distanceMeters;
    /** 操作状态 */
    private ActionStateEnum actionState;

    // 发布者信息
    private String creatorNickname;
    private String creatorAvatarUrl;
}
