package com.rally.domain.tour.player;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 与 {@code tour_player} 一行对应的不可变聚合状态。 */
public record TourPlayerState(
        Long id,
        String playerId,
        String tour,
        String firstName,
        String lastName,
        String nationality,
        LocalDate birthDate,
        String gender,
        Integer rank,
        Integer points,
        String hand,
        LocalDateTime createTime,
        LocalDateTime updateTime) {

    TourPlayerIdentity identity() {
        return TourPlayerIdentity.fromSource(tour, playerId);
    }

    TourPlayerState withGeneratedId(long generatedId) {
        return new TourPlayerState(
                generatedId,
                playerId,
                tour,
                firstName,
                lastName,
                nationality,
                birthDate,
                gender,
                rank,
                points,
                hand,
                createTime,
                updateTime);
    }

    TourPlayerState merge(TourPlayerProfilePatch patch) {
        return new TourPlayerState(
                id,
                playerId,
                tour,
                choose(patch.firstName(), firstName),
                choose(patch.lastName(), lastName),
                choose(patch.nationality(), nationality),
                choose(patch.birthDate(), birthDate),
                choose(patch.gender(), gender),
                choose(patch.rank(), rank),
                choose(patch.points(), points),
                choose(patch.hand(), hand),
                createTime,
                updateTime);
    }

    private static <T> T choose(T patch, T existing) {
        return patch == null ? existing : patch;
    }
}
