package com.rally.domain.tour.player;

import java.util.Locale;

/** I1：由规范巡回赛与来源球员编号组成的不可变自然键。 */
public record TourPlayerIdentity(Tour tour, String playerId) {

    private static final int PLAYER_ID_MAX_LENGTH = 50;

    public static TourPlayerIdentity fromSource(String sourceTour, String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        return new TourPlayerIdentity(Tour.fromSource(sourceTour), normalizedPlayerId);
    }

    public TourPlayerIdentity {
        require(tour != null, "巡回赛不能为空");
        playerId = normalizePlayerId(playerId);
    }

    public String tourCode() {
        return tour.name();
    }

    private static String normalizePlayerId(String value) {
        require(value != null && !value.isBlank(), "来源球员编号不能为空");
        String normalized = value.strip();
        require(normalized.length() <= PLAYER_ID_MAX_LENGTH,
                "来源球员编号长度不能超过 50");
        return normalized;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourPlayerDomainException(
                    TourPlayer.TOUR_PLAYER_IDENTITY_CONFLICT, message);
        }
    }

    /** 规范巡回赛代码；ATP 与 WTA 使用各自独立的来源编号空间。 */
    public enum Tour {
        ATP,
        WTA;

        static Tour fromSource(String value) {
            if (value == null || value.isBlank()) {
                throw invalid();
            }
            try {
                return valueOf(value.strip().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw invalid();
            }
        }

        private static TourPlayerDomainException invalid() {
            return new TourPlayerDomainException(
                    TourPlayer.TOUR_PLAYER_IDENTITY_CONFLICT,
                    "巡回赛只能是 ATP 或 WTA");
        }
    }
}
