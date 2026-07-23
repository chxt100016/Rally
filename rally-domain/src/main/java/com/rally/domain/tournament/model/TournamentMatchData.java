package com.rally.domain.tournament.model;

import com.rally.domain.meetup.enums.CourtSelectModeEnum;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
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
    private String courtName;
    private String courtAddress;
    private Double courtLng;
    private Double courtLat;
    private String courtCityCode;
    private String courtCityName;
    /** 球场选择模式：TEXT/MAP/FREE，与约球域保持一致 */
    private CourtSelectModeEnum courtSelectMode;
    /** 球场库ID，TEXT/MAP模式下从球场库选中时传入 */
    private String courtId;
    private LocalDateTime scheduledStartTime;
    private Integer scheduledDuration;
    private LocalDateTime scheduleSubmittedTime;
    private String meetupId;
    private String winnerId;
    private String submitterUserId;
    private LocalDateTime submittedTime;
    private String rejectReason;
    private LocalDateTime lastRebookTime;
    private TournamentMatchStatusEnum status;
    private LocalDateTime matchedTime;
    private LocalDateTime completedTime;
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
