package com.rally.domain.tour.draw;

/** C2：按既有身份分别刷新两个可空结构字段。 */
public record RefreshTourDrawStructureCommand(
        String tournamentId,
        Integer year,
        String sourceDrawType,
        Integer size,
        Integer totalRounds) {
}
