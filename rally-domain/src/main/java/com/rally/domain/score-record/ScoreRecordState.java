package com.rally.domain.meetup.scorerecord;

import java.time.LocalDateTime;

/** rally_meetup_score 一行记录对应的完整不可变聚合快照。 */
public record ScoreRecordState(
        Long id,
        String businessId,
        String meetupId,
        int setNumber,
        ScoreSetFormat setFormat,
        ScoreMatchType matchType,
        LocalDateTime meetupDate,
        ScoreLineup lineup,
        int sideAScore,
        int sideBScore,
        Integer sideATiebreakScore,
        Integer sideBTiebreakScore,
        ScoreWinSide winSide,
        String recordedBy,
        int version,
        LocalDateTime createTime,
        LocalDateTime updateTime) {
}
