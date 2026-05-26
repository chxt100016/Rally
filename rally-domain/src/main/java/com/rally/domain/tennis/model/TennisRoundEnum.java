package com.rally.domain.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TennisRoundEnum {

    FINAL("F"),
    SEMIFINAL("SF"),
    QUARTERFINAL("QF"),
    ROUND_OF_16("R16"),
    ROUND_OF_32("R32"),
    ROUND_OF_64("R64"),
    ROUND_OF_96("R96"),
    ROUND_OF_128("R128"),
    ;

    private final String roundName;

    public static String of(String str) {
        if (str == null) {
            return null;
        }

        TennisRoundEnum target = null;
        switch (str) {
            case "Final" -> target = FINAL;
            case "Semifinal" -> target = SEMIFINAL;
            case "Quarterfinal" -> target = QUARTERFINAL;
            case "Round of 16", "Round Of 16" -> target = ROUND_OF_16;
            case "Round of 32", "Round Of 32" -> target = ROUND_OF_32;
            case "Round of 64", "Round Of 64" -> target = ROUND_OF_64;
            case "Round of 96" -> target = ROUND_OF_96;
            case "Round Of 128" -> target = ROUND_OF_128;
            default -> {}
        }

        return target == null ? null : target.roundName;
    }
}
