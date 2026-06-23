package com.rally.domain.tour.model;

import lombok.Data;

@Data
public class TournamentEntryData {
    private String playerId;
    private Long drawId;
    private Short seed;
    private String entryType;
}
