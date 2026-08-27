package com.rally.domain.tour.draw;

/** 由来源赛事编号、正数年份和来源原始项目代码组成的不可变签表身份。 */
public record TourDrawIdentity(
        String tournamentId,
        int year,
        String drawType) {

    private static final int TOURNAMENT_ID_MAX_LENGTH = 50;
    private static final int DRAW_TYPE_MAX_LENGTH = 10;

    public static TourDrawIdentity fromSource(
            String tournamentId, Integer year, String sourceDrawType) {
        require(tournamentId != null && !tournamentId.isBlank(),
                "来源赛事编号不能为空");
        require(tournamentId.length() <= TOURNAMENT_ID_MAX_LENGTH,
                "来源赛事编号长度不能超过 50");
        require(year != null && year > 0,
                "签表年份必须为正数");
        require(sourceDrawType != null && !sourceDrawType.isBlank(),
                "签表类型不能为空");
        require(sourceDrawType.length() <= DRAW_TYPE_MAX_LENGTH,
                "签表类型长度不能超过 10");
        return new TourDrawIdentity(tournamentId, year, sourceDrawType);
    }

    public TourDrawIdentity {
        require(tournamentId != null && !tournamentId.isBlank(),
                "来源赛事编号不能为空");
        require(tournamentId.length() <= TOURNAMENT_ID_MAX_LENGTH,
                "来源赛事编号长度不能超过 50");
        require(year > 0, "签表年份必须为正数");
        require(drawType != null && !drawType.isBlank(), "签表类型不能为空");
        require(drawType.length() <= DRAW_TYPE_MAX_LENGTH,
                "签表类型长度不能超过 10");
    }

    public String drawTypeCode() {
        return drawType;
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new TourDrawDomainException(
                    TourDraw.TOUR_DRAW_IDENTITY_CONFLICT, message);
        }
    }
}
