package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentDisplayStatusEnum;
import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 赛事公开基础信息
 */
@Data
public class TournamentDTO {
    private String tournamentId;
    private String tournamentName;
    private String posterUrl;
    private String cityName;
    private String ntrpLevel;
    private TournamentGenderLimitEnum genderLimit;
    private Long entryFee;
    private LocalDateTime registrationStartTime;
    private LocalDateTime registrationEndTime;
    private LocalDateTime qualifierStartTime;
    private LocalDateTime qualifierEndTime;
    private Integer offlineFromRound;
    private String matchRuleDescription;
    private TournamentDisplayStatusEnum displayStatus;
    private String displayStatusShow;
}
