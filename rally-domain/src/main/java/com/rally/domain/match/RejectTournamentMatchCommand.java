package com.rally.domain.tournament.match;

import java.time.LocalDateTime;

/**
 * C8 比赛终止事实。eligible 表示活动已完成相应场景的等待时长、拒绝限额或退赛资格判断。
 */
public record RejectTournamentMatchCommand(
        TournamentMatchRejectionSource source,
        TournamentMatchRejectPhase phase,
        String reasonCode,
        String rejectedBy,
        LocalDateTime rejectedTime,
        boolean eligible,
        int version) {
}
