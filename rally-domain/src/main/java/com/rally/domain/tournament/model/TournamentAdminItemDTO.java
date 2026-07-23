package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentGenderLimitEnum;
import com.rally.domain.tournament.enums.TournamentStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 后台赛事列表项
 */
@Data
public class TournamentAdminItemDTO {
    private String tournamentId;
    private String tournamentName;
    private String posterUrl;
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
    private Integer qualifierRejectLimit;
    private Integer mainDrawRejectLimit;
    private TournamentStatusEnum status;
    /** 当前已支付锁定的正赛席位数 */
    private Integer currentFilledSlots;
    private LocalDateTime createTime;
}
