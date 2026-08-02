package com.rally.domain.tournament.model;

import com.rally.domain.meetup.model.MeetupCardDTO;
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
    private String courtBookerId;
    private String meetupId;
    /** 本场待确认或已确认的获胜方报名编号；双打同队成员共用该编号。 */
    private Integer winnerEntryNo;
    private TournamentMatchStatusEnum status;
    /** 分组人数：资格赛取赛事的 qualifierGroupSize，正赛固定为2 */
    private Integer groupSize;
    private List<MatchParticipantDTO> participants;
    /** 订场后生成的约球卡片，供对方确认赛约前查看（草稿态） */
    private MeetupCardDTO meetupCard;
    /** 订场阶段（BOOKING）对手的报名信息，供订场人参考对手场地偏好/可用时间 */
    private List<TournamentEntryDTO> opponentEntries;
    /** 最近一次打回重订信息，仅在发生过打回重订时有值。 */
    private String lastRebookBy;
    private String lastRebookReasonCode;
    private LocalDateTime lastRebookTime;
}
