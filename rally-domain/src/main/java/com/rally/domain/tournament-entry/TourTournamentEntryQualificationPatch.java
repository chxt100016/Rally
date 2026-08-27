package com.rally.domain.tour.tournamententry;

/** C1 的非空资格补丁；entryType 保持来源原文，不做枚举或缩写转换。 */
public record TourTournamentEntryQualificationPatch(Short seed, String entryType) {

    public static TourTournamentEntryQualificationPatch of(Short seed, String entryType) {
        if (entryType != null) {
            require(!entryType.isBlank(), "入围方式不能为空白文本");
            require(entryType.length() <= 10, "入围方式长度不能超过 10");
        }
        return new TourTournamentEntryQualificationPatch(seed, entryType);
    }

    public boolean isEmpty() {
        return seed == null && entryType == null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourTournamentEntryDomainException(
                    TourTournamentEntry.TOUR_ENTRY_QUALIFICATION_INVALID, message);
        }
    }
}
