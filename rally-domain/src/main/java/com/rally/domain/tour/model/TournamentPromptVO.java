package com.rally.domain.tour.model;

import lombok.Data;

@Data
public class TournamentPromptVO {
    private String tournamentId;
    private String name;
    private String category;
    private String surface;
    private String city;
    private String startDate;
    private String prompt;
}
