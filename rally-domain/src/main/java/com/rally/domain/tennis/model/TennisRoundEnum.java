package com.rally.domain.tennis.model;

import lombok.Getter;

@Getter
public enum TennisRoundEnum {

    FINAL("Final", "F"),
    SEMIFINAL("Semifinal", "SF"),
    QUARTERFINAL("Quarterfinal", "QF"),
    ROUND_OF_16("Round of 16", "R16"),
    ROUND_OF_32("Round of 32", "R32"),
    ROUND_OF_64("Round of 64", "R64"),
    ROUND_OF_96("Round of 96", "R96");

    private final String longName;
    private final String shortName;

    TennisRoundEnum(String longName, String shortName) {
        this.longName = longName;
        this.shortName = shortName;
    }

    public static String toShortName(String longName) {
        if (longName == null) {
            return null;
        }
        for (TennisRoundEnum round : values()) {
            if (round.longName.equals(longName)) {
                return round.shortName;
            }
        }
        return longName;
    }
}
