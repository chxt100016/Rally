package com.rally.db.tournament.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事报名表 PO
 */
@Data
@TableName("rally_tournament_entry")
public class TournamentEntryPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String tournamentId;
    private String userId;
    private String partnerId;
    private String preferredDistricts;
    private String courtAbility;
    private String availableTimes;
    private String stage;
    private String status;
    private String currentRound;
    private Integer qualifierRejectCount;
    private Integer mainDrawRejectCount;
    private LocalDateTime qualifiedTime;
    private LocalDateTime paidTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
