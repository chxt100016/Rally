package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.CourtAbilityEnum;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报名领域数据对象
 */
@Data
public class TournamentEntryData {
    private String bizId;
    private String tournamentId;
    private String userId;
    private String partnerId;
    private List<String> preferredDistricts;
    private CourtAbilityEnum courtAbility;
    private List<String> availableTimes;
    private TournamentEntryStageEnum stage;
    private TournamentEntryStatusEnum status;
    private TournamentRoundEnum currentRound;
    private Integer qualifierRejectCount;
    private Integer mainDrawRejectCount;
    private LocalDateTime qualifiedTime;
    private LocalDateTime paidTime;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
