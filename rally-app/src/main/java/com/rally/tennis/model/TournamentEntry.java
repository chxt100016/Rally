package com.rally.tennis.model;

import lombok.Data;

@Data
public class TournamentEntry {
    private String playerId;
    private Long drawId;
    private Short seed;
    private String entryType;
}
