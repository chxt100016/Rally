package com.rally.tennis.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum MatchStatus {
    FINISHED(List.of("F")),
    PENDING(List.of("S", "Scheduled", "U")),
    COMING(List.of("C")),
    LIVE(List.of("P")),

    ;

    private final List<String> code;



    public static MatchStatus fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.contains(code))
                .findFirst()
                .orElse(null);
    }

    public static String toStatus(String code) {
        MatchStatus status = fromCode(code);
        return status != null ? status.name() : null;
    }
}
