package com.rally.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    private String tournamentId;
    private Integer year;
    private String name;
    private String surface;
    private String category;
    private String city;
    private String country;
    private String tour;
    private Integer prizeMoney;
    private String prizeMoneyText;
    private String status;
    private LocalDate startDate;
    private LocalDate endDate;
}
