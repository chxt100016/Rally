package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.CourtAbilityEnum;
import com.rally.domain.tournament.enums.TournamentEntryStageEnum;
import com.rally.domain.tournament.enums.TournamentEntryStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.util.List;

/**
 * 报名概要返回
 */
@Data
public class TournamentEntryDTO {
    private String entryId;
    private String tournamentId;
    private String partnerId;
    private List<String> preferredDistricts;
    private CourtAbilityEnum courtAbility;
    private List<String> availableTimes;
    private TournamentEntryStageEnum stage;
    private TournamentEntryStatusEnum status;
    private TournamentRoundEnum currentRound;
}
