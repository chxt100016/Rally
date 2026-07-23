package com.rally.domain.tournament.model;

import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 当前用户进行中的比赛
 */
@Data
public class MyCurrentMatchDTO {
    private String matchId;
    private TournamentRoundEnum round;
    private List<MatchOpponentDTO> opponents;
    private String courtBookerId;
    private String courtName;
    private String courtAddress;
    private LocalDateTime scheduledStartTime;
    private Integer scheduledDuration;
    private String meetupId;
    private TournamentMatchStatusEnum status;
    private List<MatchParticipantDTO> participants;
}
