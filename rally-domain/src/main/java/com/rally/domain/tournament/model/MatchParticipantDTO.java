package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.ConfirmStatusEnum;
import lombok.Data;

/**
 * myCurrentMatch 内的参与者投影
 */
@Data
public class MatchParticipantDTO {
    private String userId;
    private String teamId;
    private ConfirmStatusEnum confirmStatus;
    private ConfirmStatusEnum resultConfirmStatus;
    private Boolean isWinner;
}
