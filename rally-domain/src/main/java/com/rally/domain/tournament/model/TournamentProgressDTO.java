package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 公开赛事进程，所有访问者可见
 */
@Data
public class TournamentProgressDTO {
    private Integer entryCount;
    private Integer totalSlots;
    private TournamentRoundEnum currentRound;
    private String currentRoundShow;
    private Integer currentRoundTotalMatches;
    private Integer currentRoundCompletedMatches;
    /** 当前轮次可晋级名额：资格赛为增赛签位数(totalSlots)，正赛为下一轮次签位数 */
    private Integer currentRoundAdvanceableSlots;
    /** 当前轮次已晋级名额：根据 entry 表下一轮次的报名记录去重统计（entryNo） */
    private Integer currentRoundAdvancedCount;
    /** 当前赛事已生成的比赛总场数（资格赛+正赛累计） */
    private Integer totalMatchCount;
    /** 赛事总进度：已完成场次 / 应打总场次（资格赛 totalSlots 场 + 正赛 totalSlots-1 场），满值为 1 */
    private BigDecimal progressRate;
    private LocalDateTime registrationEndTime;
    private LocalDateTime qualifierEndTime;
}
