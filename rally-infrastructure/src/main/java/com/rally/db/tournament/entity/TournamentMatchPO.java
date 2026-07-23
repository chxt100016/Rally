package com.rally.db.tournament.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 赛事比赛表 PO
 */
@Data
@TableName("rally_tournament_match")
public class TournamentMatchPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String tournamentId;
    private Integer matchNo;
    private String round;
    private Integer groupSize;
    private String courtBookerId;
    private LocalDateTime courtBookerSelectedTime;
    private String courtName;
    private String courtAddress;
    private Double courtLng;
    private Double courtLat;
    private String courtCityCode;
    private String courtCityName;
    /** 球场选择模式：TEXT/MAP/FREE */
    private String courtSelectMode;
    /** 球场库ID */
    private String courtId;
    private LocalDateTime scheduledStartTime;
    private BigDecimal scheduledDuration;
    private LocalDateTime scheduleSubmittedTime;
    private String meetupId;
    private String winnerId;
    private String submittedBy;
    private LocalDateTime submittedTime;
    private String rejectPhase;
    private String rejectReasonCode;
    private String rejectReasonText;
    private String rejectedBy;
    private LocalDateTime rejectedTime;
    private String lastRebookBy;
    private String lastRebookReasonCode;
    private String lastRebookReasonText;
    private LocalDateTime lastRebookTime;
    private String status;
    private LocalDateTime matchedTime;
    private LocalDateTime completedTime;

    @Version
    private Integer version;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
