package com.rally.domain.tour.draw;

/** C1：关联或建立一份签表；赛事存在性由调用方在当前用例中查定。 */
public record AssociateTourDrawCommand(
        String tournamentId,
        Integer year,
        String sourceDrawType,
        boolean tournamentCollected,
        Integer size,
        Integer totalRounds) {

    /** 兼容只有签表身份的旧调用方。 */
    public AssociateTourDrawCommand(
            String tournamentId,
            Integer year,
            String sourceDrawType,
            boolean tournamentCollected) {
        this(tournamentId, year, sourceDrawType, tournamentCollected, null, null);
    }
}
