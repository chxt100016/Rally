package com.rally.domain.tour.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public enum ScheduledAtTextEnum {
    FIXED,
    AFTER_PREVIOUS,
    NOT_EARLIER_THAN,
    UNKNOWN;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    public static String fromText(String text) {
        if (text == null) return null;
        return switch (text) {
            case "Starts At", "Starting at", "Starts at" -> FIXED.name();
            case "Followed By" -> AFTER_PREVIOUS.name();
            case "Not Before", "Not before" -> NOT_EARLIER_THAN.name();
            default -> UNKNOWN.name();
        };
    }

    /** 根据排期类型与计划时间生成展示文案 */
    public static String toShow(String type, LocalDateTime scheduledAt) {
        if (type == null) return null;
        return of(type).show(scheduledAt);
    }

    private String show(LocalDateTime scheduledAt) {
        return switch (this) {
            case FIXED -> scheduledAt != null ? scheduledAt.format(TIME_FMT) + "开赛" : null;
            case AFTER_PREVIOUS -> "随后";
            case NOT_EARLIER_THAN -> scheduledAt != null ? "不早于" + scheduledAt.format(TIME_FMT) : null;
            case UNKNOWN -> "待定";
        };
    }

    private static ScheduledAtTextEnum of(String type) {
        try {
            return valueOf(type);
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
