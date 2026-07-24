package com.rally.domain.tournament.model;

import com.rally.domain.meetup.enums.MatchTypeEnum;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事领域数据对象
 */
@Data
public class TournamentData {
    private String bizId;
    private String tournamentName;
    private String posterKey;
    private MatchTypeEnum matchType;
    private String cityCode;
    private String cityName;
    private String ntrpLevel;
    private TournamentGenderLimitEnum genderLimit;
    private Integer totalSlots;
    private Integer offlineFromRound;
    private Integer qualifierGroupSize;
    private Long entryFee;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationEndTime;
    private LocalDateTime qualifierStartTime;
    private LocalDateTime qualifierEndTime;
    private LocalDateTime endTime;
    private Integer qualifierRejectLimit;
    private Integer mainDrawRejectLimit;
    private String matchRuleDescription;
    private TournamentStatusEnum status;
    private Integer currentFilledSlots;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
