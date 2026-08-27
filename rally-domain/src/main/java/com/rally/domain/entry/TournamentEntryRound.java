package com.rally.domain.tournament.entry;

/** 资格赛与正赛轮次，顺序同时表示正赛晋级方向。 */
public enum TournamentEntryRound {
    QUALIFIER,
    ROUND_64,
    ROUND_32,
    ROUND_16,
    ROUND_8,
    ROUND_4,
    FINAL;

    public boolean isMainDrawRound() {
        return this != QUALIFIER;
    }

    public TournamentEntryRound nextMainDrawRound() {
        if (!isMainDrawRound() || this == FINAL) {
            return null;
        }
        return values()[ordinal() + 1];
    }

    public static TournamentEntryRound firstMainDrawRound(int totalSlots) {
        return switch (totalSlots) {
            case 2 -> FINAL;
            case 4 -> ROUND_4;
            case 8 -> ROUND_8;
            case 16 -> ROUND_16;
            case 32 -> ROUND_32;
            case 64 -> ROUND_64;
            default -> throw new TournamentEntryDomainException(
                    TournamentEntry.TOURNAMENT_ENTRY_PROGRESS_INVALID,
                    "正赛总签位必须是 2、4、8、16、32 或 64");
        };
    }
}
