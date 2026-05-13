package com.rally.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Match {
    private String matchId;
    private String tournamentId;
    private Integer year;
    private Long drawId;
    private Integer roundNumber;
    private String roundName;
    private String player1Id;
    private String player2Id;
    private String playerName1;
    private String playerName2;
    private String status;
    private String winnerId;
    private String court;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer durationMinutes;
    private List<SetScore> sets;
    private String scheduledAtText;
    private Integer courtSeq;
    private String description;
    private LocalDate matchDate;
}
