package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛参与者领域数据对象
 */
@Data
public class MatchParticipantData {
    private String bizId;
    private String matchId;
    private String tournamentId;
    private String userId;
    private String teamId;
    private ConfirmStatusEnum confirmStatus;
    private LocalDateTime confirmTime;
    private ConfirmStatusEnum resultConfirmStatus;
    private LocalDateTime resultConfirmTime;
    private Boolean isWinner;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
