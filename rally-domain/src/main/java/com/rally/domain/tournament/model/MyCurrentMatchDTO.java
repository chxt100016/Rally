package com.rally.domain.tournament.model;

import com.rally.domain.meetup.model.MeetupCardDTO;
import com.rally.domain.tournament.enums.TournamentMatchStatusEnum;
import com.rally.domain.tournament.enums.TournamentRoundEnum;
import lombok.Data;

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
    private String meetupId;
    private TournamentMatchStatusEnum status;
    /** 分组人数：资格赛取赛事的 qualifierGroupSize，正赛固定为2 */
    private Integer groupSize;
    private List<MatchParticipantDTO> participants;
    /** 订场后生成的约球卡片，供对方确认赛约前查看（草稿态） */
    private MeetupCardDTO meetupCard;
    /** 订场阶段（BOOKING）对手的报名信息，供订场人参考对手场地偏好/可用时间 */
    private List<TournamentEntryDTO> opponentEntries;
}
