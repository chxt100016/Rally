package com.rally.tennis.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DrawParams {
    private String tournamentId;
    private int year;
    private String tour;
}
