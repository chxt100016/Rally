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
public class Player {
    private String playerId;
    /** 所属巡回赛：ATP / WTA */
    private String tour;
    private String firstName;
    private String lastName;
    private String nationality;
    private Integer rank;
    private LocalDate birthDate;
    private String gender;
    private String hand;
}
