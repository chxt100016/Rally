package com.rally.domain.tour.draw;

import java.time.LocalDateTime;

/** 与 {@code tour_draw} 一行对应的不可变聚合状态。 */
public record TourDrawState(
        Long id,
        String tournamentId,
        int year,
        String drawType,
        Integer size,
        Integer totalRounds,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    static TourDrawState initial(
            TourDrawIdentity identity, Integer size, Integer totalRounds) {
        return new TourDrawState(
                null,
                identity.tournamentId(),
                identity.year(),
                identity.drawTypeCode(),
                size,
                totalRounds,
                null,
                null);
    }

    TourDrawIdentity identity() {
        return TourDrawIdentity.fromSource(tournamentId, year, drawType);
    }

    TourDrawState withGeneratedId(long generatedId) {
        return new TourDrawState(
                generatedId,
                tournamentId,
                year,
                drawType,
                size,
                totalRounds,
                createTime,
                updateTime);
    }

    TourDrawState withStructure(TourDrawStructure structure) {
        return new TourDrawState(
                id,
                tournamentId,
                year,
                drawType,
                structure.size(),
                structure.totalRounds(),
                createTime,
                updateTime);
    }
}
