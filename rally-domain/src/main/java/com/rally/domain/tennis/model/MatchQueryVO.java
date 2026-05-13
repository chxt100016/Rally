package com.rally.domain.tennis.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 比赛查询响应 VO
 */
@Data
public class MatchQueryVO {

    private String id;
    private String tournamentId;
    private String court;
    private Integer courtSeq;
    private String round;
    private String status;
    private String statusLabel;
    private String schedulingType;
    private String scheduledTime;
    private String date;
    private PlayerVO player1;
    private PlayerVO player2;
    private LocalDateTime startedAt;
    private LocalDateTime scheduledAt;
    private List<SetScoreVO> sets;
    private Integer currentSet;
    private String currentSetScore;
    private String winnerId;
    private String duration;
}
