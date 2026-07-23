package com.rally.domain.tournament.enums;

/**
 * 赛事轮次枚举
 */
public enum TournamentRoundEnum {
    QUALIFIER,
    ROUND_64,
    ROUND_32,
    ROUND_16,
    ROUND_8,
    ROUND_4,
    FINAL;

    /** 资格赛晋级正赛后的首个轮次：totalSlots(16/32/64) 直接对应 ROUND_16/ROUND_32/ROUND_64 */
    public static TournamentRoundEnum firstMainRound(int totalSlots) {
        return switch (totalSlots) {
            case 16 -> ROUND_16;
            case 32 -> ROUND_32;
            case 64 -> ROUND_64;
            default -> throw new IllegalArgumentException("totalSlots 只能是 16/32/64");
        };
    }
}
