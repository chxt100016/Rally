package com.rally.db.tournament.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事比赛参与者表 PO
 */
@Data
@TableName("rally_tournament_match_participant")
public class MatchParticipantPO {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizId;
    private String matchId;
    private String tournamentId;
    private String userId;
    private String teamId;
    private String confirmStatus;
    private LocalDateTime confirmTime;
    private String resultConfirmStatus;
    private LocalDateTime resultConfirmTime;
    private Boolean isWinner;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
