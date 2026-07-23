package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公开赛事进程，所有访问者可见
 */
@Data
public class TournamentProgressDTO {
    private Integer entryCount;
    private Integer currentFilledSlots;
    private Integer totalSlots;
    private TournamentRoundEnum currentRound;
    private Integer currentRoundTotalMatches;
    private Integer currentRoundCompletedMatches;
    /** 当前赛事已生成的比赛总场数（资格赛+正赛累计） */
    private Integer totalMatchCount;
    private LocalDateTime registrationEndTime;
    private LocalDateTime qualifierEndTime;
}
