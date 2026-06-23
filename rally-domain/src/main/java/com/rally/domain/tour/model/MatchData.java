package com.rally.domain.tour.model;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 比赛数据模型（domain 层）
 */
@Data
public class MatchData {

    private String matchId;
    /** 对应 tour_match.id，用于关联盘分数据 */
    private Long tourMatchId;
    private Integer year;
    private Long drawId;
    /** 比赛序号，二叉树结构：第1轮最大，决赛=1，父节点 = floor(matchIndex/2) */
    private Integer matchIndex;
    private Integer roundNumber;
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
