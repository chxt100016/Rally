package com.rally.domain.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TennisRoundEnum {

    FINAL("F", "决赛"),
    SEMIFINAL("SF", "半决赛"),
    QUARTERFINAL("QF", "8强"),
    ROUND_OF_16("R16", "16强"),
    ROUND_OF_32("R32", "32强"),
    ROUND_OF_64("R64", "64强"),
    ROUND_OF_96("R96", "96强"),
    ROUND_OF_128("R128", "128强"),
    ;

    private final String roundName;
    /** 中文显示名 */
    private final String label;

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
            case "Round of 128", "Round Of 128" -> target = ROUND_OF_128;
            default -> {}
        }

        return target == null ? null : target.roundName;
    }

    /** 根据 roundName（如 "F"、"SF"）返回中文 label */
    public static String labelOf(String roundName) {
        if (roundName == null) {
            return null;
        }
        for (TennisRoundEnum e : values()) {
            if (e.roundName.equals(roundName)) {
                return e.label;
            }
        }
        return null;
    }
}
