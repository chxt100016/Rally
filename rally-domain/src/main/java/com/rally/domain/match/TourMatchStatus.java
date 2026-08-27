package com.rally.domain.tour.match;

import java.util.Locale;

/** 可被后续来源任意纠正的比赛快照状态。 */
public enum TourMatchStatus {
    UNKNOWN,
    PENDING,
    COMING,
    LIVE,
    FINISHED;

    /** 空值或无法识别的来源状态不构成补丁。 */
    static TourMatchStatus recognizePatch(String sourceStatus) {
        if (sourceStatus == null || sourceStatus.isBlank()) {
            return null;
        }
        try {
            TourMatchStatus status = valueOf(sourceStatus.strip().toUpperCase(Locale.ROOT));
            return status == UNKNOWN ? null : status;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    /** 历史空值或未识别值在聚合内统一视为 UNKNOWN。 */
    static TourMatchStatus restore(String storedStatus) {
        TourMatchStatus recognized = recognizePatch(storedStatus);
        return recognized == null ? UNKNOWN : recognized;
    }
}
