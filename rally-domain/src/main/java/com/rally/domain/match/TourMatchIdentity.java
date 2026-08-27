package com.rally.domain.tour.match;

/** 由已保存签表和来源比赛号组成的不可变自然键。 */
public record TourMatchIdentity(long drawId, String matchId) {

    private static final int MATCH_ID_MAX_LENGTH = 50;

    public static TourMatchIdentity fromSource(Long drawId, String matchId) {
        require(drawId != null && drawId > 0, "签表 id 必须为正数");
        String normalizedMatchId = normalizeRequired(matchId);
        require(normalizedMatchId.length() <= MATCH_ID_MAX_LENGTH,
                "来源比赛号长度不能超过 50");
        return new TourMatchIdentity(drawId, normalizedMatchId);
    }

    public TourMatchIdentity {
        require(drawId > 0, "签表 id 必须为正数");
        matchId = normalizeRequired(matchId);
        require(matchId.length() <= MATCH_ID_MAX_LENGTH,
                "来源比赛号长度不能超过 50");
    }

    /** I3：只由 match_id 中的全部数字派生非负比赛序号。 */
    public Integer deriveMatchIndex() {
        StringBuilder digits = new StringBuilder();
        for (int index = 0; index < matchId.length(); index++) {
            char character = matchId.charAt(index);
            if (Character.isDigit(character)) {
                digits.append(character);
            }
        }
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(digits.toString());
        } catch (NumberFormatException exception) {
            throw new TourMatchDomainException(
                    TourMatch.TOUR_MATCH_INDEX_CONFLICT,
                    "match_id 的数字部分无法表示有效比赛序号");
        }
    }

    private static String normalizeRequired(String value) {
        require(value != null && !value.isBlank(), "来源比赛号不能为空");
        return value.strip();
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourMatchDomainException(
                    TourMatch.TOUR_MATCH_IDENTITY_CONFLICT, message);
        }
    }
}
