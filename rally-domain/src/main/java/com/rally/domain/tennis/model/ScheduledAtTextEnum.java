package com.rally.domain.tennis.model;

public enum ScheduledAtTextEnum {
    FIXED,
    AFTER_PREVIOUS,
    NOT_EARLIER_THAN,
    UNKNOWN;

    public static String fromText(String text) {
        if (text == null) return null;
        return switch (text) {
            case "Starts At", "Starting at", "Starts at" -> FIXED.name();
            case "Followed By" -> AFTER_PREVIOUS.name();
            case "Not Before", "Not before" -> NOT_EARLIER_THAN.name();
            default -> UNKNOWN.name();
        };
    }
}
