package com.rally.db.tour.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "tour_match")
public class TourMatchPO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String matchId;
    private Integer matchIndex;
    private Long drawId;
    private String tournamentId;
    private Integer year;
    private Integer roundNumber;
    private String roundName;
    private String player1Id;
    private String player2Id;
    private String winnerId;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String court;
    private String status;
    private Integer durationMinutes;
    private String scheduledAtText;
    private Integer courtSeq;
    private String description;
    private LocalDate matchDate;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
