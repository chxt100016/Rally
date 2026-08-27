package com.rally.domain.tour.player;

import java.time.LocalDate;

/** C1：新增或用任意非空来源字段刷新一名职业球员的资料。 */
public record RefreshTourPlayerCommand(
        String tour,
        String playerId,
        String firstName,
        String lastName,
        String nationality,
        String birthDate,
        String gender,
        Integer rank,
        Integer points,
        String hand) {

    /** 供已经完成日期解析的采集端构造同一条 C1 命令。 */
    public static RefreshTourPlayerCommand fromParsedBirthDate(
            String tour,
            String playerId,
            String firstName,
            String lastName,
            String nationality,
            LocalDate birthDate,
            String gender,
            Integer rank,
            Integer points,
            String hand) {
        return new RefreshTourPlayerCommand(
                tour,
                playerId,
                firstName,
                lastName,
                nationality,
                birthDate == null ? null : birthDate.toString(),
                gender,
                rank,
                points,
                hand);
    }
}
