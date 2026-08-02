package com.rally.domain.tournament.model;

import com.rally.domain.meetup.model.MeetupCardDTO;
import lombok.Data;

/** 赛事线下赛活动信息。 */
@Data
public class TournamentOfflineDTO {
    private String meetupId;
    private MeetupCardDTO meetupCard;
}
