package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRejectPhaseEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 比赛领域数据对象
 */
@Data
public class TournamentMatchData {
    private String bizId;
    private String tournamentId;
    private Integer matchNo;
    private TournamentRoundEnum round;
    private Integer groupSize;
    private String courtBookerId;
    private LocalDateTime courtBookerSelectedTime;
    private LocalDateTime scheduleSubmittedTime;
    /** 关联的约球ID（订场时创建的草稿约球，场地/时间/费用等均以约球为准） */
    private String meetupId;
    private Integer winnerEntryNo;
    private String submitterUserId;
    private LocalDateTime submittedTime;
    private TournamentRejectPhaseEnum rejectPhase;
    private String rejectReasonCode;
    private String rejectedBy;
    private LocalDateTime rejectedTime;
    /** 最近一次打回重订的用户及理由，用于订场人重新提交时展示原因。 */
    private String lastRebookBy;
    private String lastRebookReasonCode;
    private LocalDateTime lastRebookTime;
    private TournamentMatchStatusEnum status;
    private LocalDateTime matchedTime;
    private LocalDateTime completedTime;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
