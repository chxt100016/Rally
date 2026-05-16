package com.rally.domain.tennis.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 比赛数据模型（domain 层）
 */
@Data
public class MatchData {

    private String matchId;
    /** 对应 tennis_match.id，用于关联盘分数据 */
    private Long tennisMatchId;
    private String tournamentId;
    private String player1Id;
    private String player2Id;
    private String winnerId;
    private String roundName;
    private String court;
    private Integer courtSeq;
    private String status;
    private Integer durationMinutes;
    private String scheduledAtText;
    private LocalDate matchDate;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
}
