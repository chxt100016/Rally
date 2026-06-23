package com.rally.domain.tour.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TournamentData {
    private Long id;
    private String tournamentId;
    private Integer year;
    private String name;
    private String tour;
    private String category;
    private String surface;
    private String city;
    private String country;
    private Integer prizeMoney;
    private String prizeMoneyText;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
    private String imagePath;
    private String backgroundPath;
}
