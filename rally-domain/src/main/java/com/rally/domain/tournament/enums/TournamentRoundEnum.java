package com.rally.domain.tournament.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 赛事轮次枚举
 */
@AllArgsConstructor
@Getter
public enum TournamentRoundEnum {
    QUALIFIER("资格赛", 0),
    ROUND_64("64强", 64),
    ROUND_32("32强", 32),
    ROUND_16("16强", 16),
    ROUND_8("8强", 8),
    ROUND_4("4强", 4),
    FINAL("决赛", 2);

    public final String label;

    /** 该轮参赛签位数，QUALIFIER 无固定签位数（按 totalSlots 单独计算） */
    public final int slotCount;

    /** 资格赛晋级正赛后的首个轮次：totalSlots(16/32/64) 直接对应 ROUND_16/ROUND_32/ROUND_64 */
    public static TournamentRoundEnum firstMainRound(int totalSlots) {
        return switch (totalSlots) {
            case 16 -> ROUND_16;
            case 32 -> ROUND_32;
            case 64 -> ROUND_64;
            default -> throw new IllegalArgumentException("totalSlots 只能是 16/32/64");
        };
    }

    /** 该轮应打总场次：资格赛每场出一个正赛签位，场次=totalSlots；正赛每轮场次=该轮签位数/2 */
    public int requiredMatchCount(int totalSlots) {
        return this == QUALIFIER ? totalSlots : slotCount / 2;
    }

    /** 下一轮次，FINAL 无下一轮返回 null */
    public TournamentRoundEnum nextRound() {
        TournamentRoundEnum[] values = values();
        int idx = this.ordinal();
        return idx + 1 < values.length ? values[idx + 1] : null;
    }
}
