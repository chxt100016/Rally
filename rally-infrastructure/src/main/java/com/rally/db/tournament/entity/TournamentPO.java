package com.rally.db.tournament.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事主表 PO
 */
@Data
@TableName("rally_tournament")
public class TournamentPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String tournamentName;
    private String posterKey;
    private String rulePosterKey;
    private String wechatGroupQrCodeKey;
    private String matchType;
    private String cityCode;
    private String cityName;
    private String ntrpLevel;
    private String genderLimit;
    private Integer totalSlots;
    private String offlineFromRound;
    private Integer qualifierGroupSize;
    private Long entryFee;
    private String prizeMoney;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationEndTime;
    private LocalDateTime qualifierStartTime;
    private LocalDateTime qualifierEndTime;
    private LocalDateTime endTime;
    private Integer qualifierRejectLimit;
    private Integer mainDrawRejectLimit;
    private String matchRuleDescription;
    private String extData;
    private String status;
    private Integer currentFilledSlots;
    private String currentRound;
    private Integer championEntryNo;
    private String offlineMeetupId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
