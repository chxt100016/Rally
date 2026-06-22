package com.rally.domain.tennis.model;

import lombok.Data;

import java.time.LocalDate;

@Data
public class PlayerData {
    private String playerId;
    private String tour;
    private String firstName;
    private String lastName;
    private String nationality;
    private Integer rank;
    private Integer points;
    private LocalDate birthDate;
    private String gender;
    private String hand;
}
