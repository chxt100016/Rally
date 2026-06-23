package com.rally.domain.tour.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
@AllArgsConstructor
public enum MatchStatus {
    FINISHED(List.of("F", "Completed"), "已结束"),
    PENDING(List.of("S", "Scheduled", "U"), "待开始"),
    COMING(List.of("C"), "待定"),
    LIVE(List.of("P"), "进行中"),

    ;

    private final List<String> code;
    private final String label;



    public static MatchStatus fromCode(String code) {
        if (code == null) return null;
        return Arrays.stream(values())
                .filter(s -> s.code.contains(code))
                .findFirst()
                .orElse(null);
    }

    public static MatchStatus fromName(String name) {
        if (name == null) return null;
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String toStatus(String code) {
        MatchStatus status = fromCode(code);
        return status != null ? status.name() : null;
    }
}
