package com.rally.domain.tour.model;

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
    private String roundShow;
    private String status;
    private String statusLabel;
    private String scheduledShow;
    private String date;
    private PlayerVO player1;
    private PlayerVO player2;
    private LocalDateTime startedAt;
    private LocalDateTime scheduledAt;
    private List<SetScoreVO> sets;
    private Integer currentSet;
    private String currentSetScore;
    private String winnerId;
    private String durationShow;
}
