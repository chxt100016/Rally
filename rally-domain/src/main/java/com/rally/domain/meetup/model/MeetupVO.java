package com.rally.domain.meetup.model;

import com.rally.domain.meetup.enums.*;
import com.rally.domain.recap.model.RecapDTO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 约球详情/卡片视图
 */
@Data
public class MeetupVO {
    private String meetupId;
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
    private String levelValue;
    private GenderLimitEnum genderLimit;
    private JoinModeEnum joinMode;
    private List<CostItem> costItems;
    private MeetupStatusEnum status;
    private LocalDateTime createTime;

    // 计算字段
    /** 每人费用（分） */
    private Integer perPersonCost;
    /** 距离（米），距离排序时返回 */
    private Double distanceMeters;
    /** 操作状态 */
    private ActionStateEnum actionState;
    /** 退出是否扣分 */
    private Boolean quitWillPenalize;

    // 发布者信息
    private String creatorNickname;
    private String creatorAvatarUrl;
    private BigDecimal creatorNtrp;

    // 参与者列表
    private List<ParticipantDTO> participants;

    // 赛后收集（活动结束时返回）
    private RecapDTO recap;
}
