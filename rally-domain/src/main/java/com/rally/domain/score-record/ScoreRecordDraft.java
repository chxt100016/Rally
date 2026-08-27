package com.rally.domain.meetup.scorerecord;

import java.time.LocalDateTime;

/** C1/C2 共享的输入载荷；C2 按 MyBatis-Plus 默认规则仅更新非空字段。 */
public record ScoreRecordDraft(
        int setNumber,
        ScoreSetFormat setFormat,
        ScoreMatchType matchType,
        LocalDateTime meetupDate,
        ScoreLineup lineup,
        Integer sideAScore,
        Integer sideBScore,
        Integer sideATiebreakScore,
        Integer sideBTiebreakScore,
        String recordedBy) {
}
